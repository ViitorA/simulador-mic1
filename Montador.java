import java.util.HashMap;
import java.util.Map;

public class Montador {
    public static int[] montar(String codigoFonte) {
        String[] linhas = codigoFonte.split("\n");
        // Aumentado para 4096 para cobrir toda a memória endereçável do MAC-1
        int[] memoria = new int[4096]; 
        int end = 0; // Endereço atual para instruções sequenciais
        
        for(String linha : linhas) {
            linha = linha.trim().toUpperCase();
            if(linha.isEmpty() || linha.startsWith(";") || linha.startsWith("//")) continue;
            
            // Remove comentários inline (ex: "LODD 10 // comentario")
            if(linha.contains("//")) {
                linha = linha.split("//")[0].trim();
            }

            String[] partes = linha.split("\\s+");
            if(partes.length == 0) continue;

            String mnemonico = partes[0];
            
            // --- NOVA FUNCIONALIDADE: Definição de Dados (DW) ---
            // Permite escrever "DW 100 10" para colocar o valor 10 no endereço 100
            if(mnemonico.equals("DW")) {
                if(partes.length >= 3) {
                    try {
                        int addr = Integer.parseInt(partes[1]);
                        int val = Integer.parseInt(partes[2]);
                        if(addr >= 0 && addr < 4096) {
                            memoria[addr] = val;
                        }
                    } catch(Exception e) {}
                }
                continue; // Pula o processamento de instrução normal, pois é dado
            }

            // Processamento normal de instruções
            int arg = 0;
            if(partes.length > 1) {
                try {
                    arg = Integer.parseInt(partes[1]);
                } catch (NumberFormatException e) { arg = 0; }
            }
            
            int opcode = 0;
            boolean instrucaoValida = true;

            switch(mnemonico) {
                case "LODD": opcode = 0x0; break; 
                case "STOD": opcode = 0x1; break; 
                case "ADDD": opcode = 0x2; break; 
                case "SUBD": opcode = 0x3; break; 
                case "JPOS": opcode = 0x4; break; 
                case "JZER": opcode = 0x5; break; 
                case "JUMP": opcode = 0x6; break; 
                case "LOCO": opcode = 0x7; break; 
                case "LODL": opcode = 0x8; break; 
                case "STOL": opcode = 0x9; break; 
                case "ADDL": opcode = 0xA; break; 
                case "SUBL": opcode = 0xB; break; 
                case "JNEG": opcode = 0xC; break; 
                case "JNZE": opcode = 0xD; break; 
                case "CALL": opcode = 0xE; break; 
                case "HALT": opcode = 0xF; break; 
                default: instrucaoValida = false;
            }
            
            if(instrucaoValida && end < 4096) {
                int instrucao = (opcode << 12) | (arg & 0xFFF);
                memoria[end++] = instrucao;
            }
        }
        return memoria;
    }
}