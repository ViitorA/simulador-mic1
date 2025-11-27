import javax.swing.*;
import java.awt.*;

public class PainelDatapath extends JPanel {
    private Mic1Simulador sim;

    public PainelDatapath(Mic1Simulador sim) {
        this.sim = sim;
        this.setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2));

        int w = getWidth();
        int h = getHeight();
        // Ajuste do centro para garantir espaço para a Cache na direita
        int xBase = Math.max(50, w / 2 - 250); 

        g2.setColor(Color.DARK_GRAY);
        g2.drawString("Visualizador de Caminho de Dados MIC-1", 20, 20);
        
        // Mostra qual instrução MAC-1 está sendo executada agora (Decodificação)
        String instrucaoAtual = getMnemonico((sim.reg.MDR >> 12) & 0xF);
        g2.drawString("Instrucao MAC-1 Atual (IR): " + instrucaoAtual, 20, 40);

        // --- CORREÇÃO: Cache movida para xBase + 420 para não cobrir o AC ---
        desenharCache(g2, xBase + 420, 80);

        // --- Registradores (Layout em U) ---
        int yTop = 80;
        // Espaçamento de 80px entre registradores
        desenharReg(g2, "PC", sim.reg.PC, xBase, yTop, sim.busB_Ativo); 
        desenharReg(g2, "SP", sim.reg.SP, xBase+80, yTop, false);
        desenharReg(g2, "MDR", sim.reg.MDR, xBase+160, yTop, sim.mem_Leitura || sim.mem_Escrita);
        desenharReg(g2, "MAR", sim.reg.MAR, xBase+240, yTop, sim.mem_Leitura || sim.mem_Escrita);
        // O AC termina em xBase + 320 + 70 = 390. A Cache começa em 420. Sem sobreposição.
        desenharReg(g2, "AC", sim.reg.AC, xBase+320, yTop, true);

        // --- ALU e Parte Inferior ---
        int yALU = 300;
        desenharReg(g2, "H", sim.reg.H, xBase+100, yALU-60, true);
        
        Polygon alu = new Polygon();
        alu.addPoint(xBase+80, yALU); alu.addPoint(xBase+240, yALU);
        alu.addPoint(xBase+200, yALU+60); alu.addPoint(xBase+120, yALU+60);
        g2.setColor(sim.alu_Ativa ? new Color(255, 200, 200) : Color.LIGHT_GRAY);
        g2.fillPolygon(alu);
        g2.setColor(Color.BLACK);
        g2.drawPolygon(alu);
        g2.drawString("ALU", xBase+145, yALU+35);
        g2.drawString(sim.reg.Z ? "Z=1" : "Z=0", xBase+250, yALU+20);
        g2.drawString(sim.reg.N ? "N=1" : "N=0", xBase+250, yALU+40);

        // --- Barramentos ---
        g2.setColor(Color.GRAY);
        g2.drawLine(xBase+160, yALU+60, xBase+160, yALU+100); // Saída ALU
        g2.drawLine(xBase-20, yALU+100, xBase+450, yALU+100); // Barramento C (Horizontal)
        
        // Linhas verticais de retorno
        g2.drawLine(xBase+40, yTop+40, xBase+40, yALU+100); // PC
        g2.drawLine(xBase+360, yTop+40, xBase+360, yALU+100); // AC
    }

    private void desenharReg(Graphics2D g, String nome, int valor, int x, int y, boolean ativo) {
        g.setColor(ativo ? new Color(255, 255, 200) : new Color(230, 230, 250));
        g.fillRoundRect(x, y, 70, 40, 10, 10);
        g.setColor(ativo ? Color.RED : Color.BLACK);
        g.setStroke(new BasicStroke(ativo ? 3 : 1));
        g.drawRoundRect(x, y, 70, 40, 10, 10);
        g.setStroke(new BasicStroke(1));
        
        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 12));
        g.drawString(nome, x+10, y+15);
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString(""+valor, x+10, y+35);
    }

    private void desenharCache(Graphics2D g, int x, int y) {
        g.setColor(new Color(240, 248, 255)); // Fundo azul claro para destacar
        g.fillRect(x, y, 150, 140);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, 150, 140);
        
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.drawString("Cache L1 (Dados)", x+10, y+20);
        
        // Indicador de Status (Semáforo)
        g.setColor(sim.cache.ultimaCor);
        g.fillOval(x+115, y+5, 25, 25);
        g.setColor(Color.BLACK);
        g.drawOval(x+115, y+5, 25, 25);
        
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Acertos: " + sim.cache.hits, x+10, y+50);
        g.drawString("Falhas : " + sim.cache.misses, x+10, y+70);
        
        g.setFont(new Font("Arial", Font.ITALIC, 11));
        g.drawString("Ultima Operacao:", x+10, y+100);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString(sim.cache.ultimoStatus, x+10, y+120);
    }

    private String getMnemonico(int op) {
        String[] ops = {"LODD","STOD","ADDD","SUBD","JPOS","JZER","JUMP","LOCO",
                        "LODL","STOL","ADDL","SUBL","JNEG","JNZE","CALL","HALT"};
        if(op >= 0 && op < ops.length) return ops[op];
        return "???";
    }
}