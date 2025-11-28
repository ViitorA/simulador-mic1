
import java.util.HashMap;
import java.util.Map;

public class Mic1Simulador {
    public Registradores reg = new Registradores();
    public MicroInstrucao[] controlStore = new MicroInstrucao[512];
    public String[] codigoFonteMicro = new String[512];
    
    public Map<Integer, Integer> memoria = new HashMap<>();
    public Cache cache = new Cache(8);

    public boolean busB_Ativo = false;
    public boolean busC_Ativo = false;
    public boolean alu_Ativa = false;
    public boolean mem_Leitura = false;
    public boolean mem_Escrita = false;
    
    public static final int B_MDR=0, B_PC=1, B_MBR=2, B_MBRU=3, B_SP=4, B_LV=5, B_CPP=6, B_TOS=7, B_OPC=8;
    public static final int C_H=1, C_OPC=2, C_TOS=4, C_CPP=8, C_LV=16, C_SP=32, C_PC=64, C_MDR=128, C_MAR=256, C_AC=512; 

    public Mic1Simulador() {}

    public void compilarMicrocodigo(String texto) throws Exception {
        for(int i=0; i<512; i++) { controlStore[i] = null; codigoFonteMicro[i] = ""; }
        String[] linhas = texto.split("\n");
        for(String linha : linhas) {
            linha = linha.trim();
            if(linha.isEmpty() || linha.startsWith("//")) continue;
            try {
                String[] partes = linha.split(":");
                int end = Integer.parseInt(partes[0].trim());
                codigoFonteMicro[end] = linha;
                String resto = partes[1].trim();
                String[] cmds = resto.split(";");
                int cBus=0, bBus=0, alu=0, mem=0, next=0, jam=0;
                for(String cmd : cmds) {
                    cmd = cmd.trim().toUpperCase();
                    if(cmd.startsWith("GOTO")) {
                        if(cmd.contains("(MBR)")) jam = MicroInstrucao.JMPC;
                        else next = Integer.parseInt(cmd.replace("GOTO","").trim());
                    }
                    else if(cmd.equals("RD")) mem |= MicroInstrucao.READ;
                    else if(cmd.equals("WR")) mem |= MicroInstrucao.WRITE;
                    else if(cmd.contains("=")) {
                        String[] eq = cmd.split("=");
                        String dest = eq[0]; String op = eq[1];
                        if(dest.contains("H")) cBus|=C_H; if(dest.contains("PC")) cBus|=C_PC;
                        if(dest.contains("MDR")) cBus|=C_MDR; if(dest.contains("MAR")) cBus|=C_MAR;
                        if(dest.contains("AC")) cBus|=C_AC; if(dest.contains("SP")) cBus|=C_SP;
                        
                        if(op.contains("MDR")) bBus=B_MDR; else if(op.contains("PC")) bBus=B_PC;
                        else if(op.contains("MBR")) bBus=B_MBR; else if(op.contains("SP")) bBus=B_SP;
                        else if(op.equals("AC")) { bBus=B_MBR; } // Hack para permitir H = AC visualmente
                        
                        if(op.contains("+") && op.contains("1")) alu=7;
                        else if(op.contains("+")) alu=4;
                        else if(op.contains("INV")) alu=2;
                        else if(op.equals("B") || op.equals("MDR") || op.equals("PC") || op.equals("MBR") || op.equals("AC")) alu=1;
                    }
                }
                controlStore[end] = new MicroInstrucao(next, jam, alu, cBus, mem, bBus);
            } catch(Exception e) {}
        }
    }

    public void step() {
        MicroInstrucao ui = controlStore[reg.MPC];
        if(ui == null) return;

        busB_Ativo = true;
        busC_Ativo = (ui.cBus != 0);
        alu_Ativa = true;
        mem_Leitura = (ui.mem & MicroInstrucao.READ) != 0;
        mem_Escrita = (ui.mem & MicroInstrucao.WRITE) != 0;

        int valA = reg.H;
        int valB = 0;
        switch(ui.bBus) {
            case B_MDR: valB = reg.MDR; break;
            case B_PC:  valB = reg.PC; break;
            case B_MBR: valB = reg.MBR; break;
            case B_SP:  valB = reg.SP; break;
        }
        // Hack: Se a instrução pediu AC (que não está no bus B padrão do MIC-1), usamos uma rota virtual
        if (ui.bBus == B_MBR && (ui.cBus & C_MDR) != 0) valB = reg.AC; // STOD
        if (ui.bBus == B_MBR && (ui.cBus & C_H) != 0 && codigoFonteMicro[reg.MPC].contains("H = AC")) valB = reg.AC; // Soma

        int result = 0;
        switch(ui.alu) {
            case 0: result = valA; break;
            case 1: result = valB; break;
            case 2: result = ~valA; break;
            case 4: result = valA + valB; break;
            case 7: result = valB + 1; break;
            case 11: result = valA & valB; break;
        }
        
        reg.Z = (result == 0);
        reg.N = (result < 0);

        // --- CORREÇÃO IMPORTANTE: MAR deve ter apenas 12 bits ---
        if((ui.cBus & C_MAR) != 0) reg.MAR = result & 0xFFF; 
        
        if((ui.cBus & C_MDR) != 0) reg.MDR = result;
        if((ui.cBus & C_PC)  != 0) reg.PC  = result;
        if((ui.cBus & C_H)   != 0) reg.H   = result;
        if((ui.cBus & C_AC)  != 0) reg.AC  = result;
        if((ui.cBus & C_SP)  != 0) reg.SP  = result;

        if(mem_Leitura) {
            reg.MDR = cache.ler(reg.MAR, memoria);
            reg.MBR = reg.MDR; 
        }
        if(mem_Escrita) {
            cache.escrever(reg.MAR, reg.MDR, memoria);
        }

        int next = ui.nextAddress;
        if((ui.jam & MicroInstrucao.JMPC) != 0) {
            // Mapeamento de Opcode MAC-1 para Endereço MIC-1
            int opcode = (reg.MBR >> 12) & 0xF;
            if(opcode == 0) next = 10;      // LODD
            else if(opcode == 1) next = 20; // STOD
            else if(opcode == 2) next = 30; // ADDD
            else if(opcode == 3) next = 40; // SUBD
            else if(opcode == 4) next = 50; // JPOS
            else if(opcode == 5) next = 70; // JZER
            else if(opcode == 6) next = 60; // JUMP
        }
        reg.MPC = next;
    }

    public void carregarPrograma(int[] codigoMaquina) {
        cache.reset();
        memoria.clear();
        for(int i=0; i<codigoMaquina.length; i++) memoria.put(i, codigoMaquina[i]);
    }
}