import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends JFrame {
    private Mic1Simulador simulador;
    private PainelDatapath painelVisual;
    private JTextArea areaMicrocodigo;
    private JTextArea areaAssembly;
    private JTextArea areaLog;
    private JButton btnStep, btnRunStop, btnReset;
    private JSlider sliderSpeed;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    public Main() {
        super("Simulador MIC-1/MAC-1 Profissional - Grupo 6");
        
        simulador = new Mic1Simulador();
        painelVisual = new PainelDatapath(simulador);
        
        JSplitPane splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.add(splitMain, BorderLayout.CENTER);

        JTabbedPane abasEditores = new JTabbedPane();
        
        JPanel painelMicro = new JPanel(new BorderLayout());
        String codigoDefault = getMicrocodigoDefault();
        areaMicrocodigo = new JTextArea(codigoDefault);
        areaMicrocodigo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        painelMicro.add(new JScrollPane(areaMicrocodigo), BorderLayout.CENTER);
        JButton btnCompilarMicro = new JButton("1. Compilar Microcodigo (Hardware)");
        painelMicro.add(btnCompilarMicro, BorderLayout.SOUTH);
        abasEditores.addTab("Microcodigo (Controle MIC-1)", null, painelMicro, "Define o comportamento do hardware");

        JPanel painelAssembly = new JPanel(new BorderLayout());
        
        // --- EXEMPLO MAC-1 AVANÇADO ---
        String assemblyDefault = 
            "// PROGRAMA: CONTAGEM REGRESSIVA\n" +
            "// 1. Inicia X = 5\n" +
            "// 2. Subtrai 1 repetidamente\n" +
            "// 3. Para quando X chegar a 0\n" +
            "\n" +
            "LODD 100   // Carrega X (5) no AC\n" +
            "\n" +
            "// --- LOOP ---\n" +
            "JZER 10    // Se AC for zero, pula para o fim (linha 10)\n" +
            "SUBD 101   // Subtrai 1\n" +
            "STOD 100   // Salva novo valor de X\n" +
            "JUMP 1     // Volta para verificar o JZER (End 1)\n" +
            "\n" +
            "// --- FIM ---\n" +
            "JUMP 10    // Fica preso aqui (Halt)\n" +
            "\n" +
            "// --- VARIAVEIS ---\n" +
            "DW 100 5   // Variavel X = 5\n" +
            "DW 101 1   // Constante = 1";
            
        areaAssembly = new JTextArea(assemblyDefault);
        areaAssembly.setFont(new Font("Monospaced", Font.PLAIN, 14));
        areaAssembly.setForeground(new Color(0, 0, 100));
        painelAssembly.add(new JScrollPane(areaAssembly), BorderLayout.CENTER);
        
        JPanel panelBotoesAssembly = new JPanel(new GridLayout(1, 2));
        JButton btnCarregarAssembly = new JButton("2. Montar & Carregar RAM");
        panelBotoesAssembly.add(btnCarregarAssembly);
        painelAssembly.add(panelBotoesAssembly, BorderLayout.SOUTH);
        
        abasEditores.addTab("Programa (Assembly MAC-1)", null, painelAssembly, "Escreva o programa do usuario");
        
        splitMain.setLeftComponent(abasEditores);

        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.add(painelVisual, BorderLayout.CENTER);
        
        JPanel controles = new JPanel(new FlowLayout());
        btnReset = new JButton("Reiniciar");
        btnStep = new JButton("Passo");
        btnRunStop = new JButton("Executar");
        
        sliderSpeed = new JSlider(100, 1500, 500);
        controles.add(new JLabel("Atraso (ms):"));
        controles.add(sliderSpeed);
        controles.add(btnReset);
        controles.add(btnStep);
        controles.add(btnRunStop);
        
        areaLog = new JTextArea(4, 40);
        areaLog.setEditable(false);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        painelDireito.add(controles, BorderLayout.NORTH);
        painelDireito.add(new JScrollPane(areaLog), BorderLayout.SOUTH);
        
        splitMain.setRightComponent(painelDireito);
        splitMain.setDividerLocation(400);

        compilarMicrocodigo(); 

        btnCompilarMicro.addActionListener(e -> compilarMicrocodigo());
        
        btnCarregarAssembly.addActionListener(e -> {
            try {
                int[] obj = Montador.montar(areaAssembly.getText());
                simulador.carregarPrograma(obj);
                areaLog.append("\nPrograma montado com sucesso.");
                painelVisual.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro no Assembly.");
            }
        });

        btnStep.addActionListener(e -> step());
        btnRunStop.addActionListener(e -> toggleRun());
        btnReset.addActionListener(e -> {
            stopRunning();
            simulador.reg.reset();
            simulador.cache.reset();
            areaLog.setText("Sistema Zerado. Recarregue o programa.");
            painelVisual.repaint();
        });

        this.setSize(1200, 750);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void compilarMicrocodigo() {
        try {
            simulador.compilarMicrocodigo(areaMicrocodigo.getText());
            areaLog.setText("Microcodigo compilado e carregado.");
            simulador.reg.MPC = 0;
        } catch (Exception e) { e.printStackTrace(); }
    }

    private String getMicrocodigoDefault() {
        return 
            "// --- FETCH (Busca Padrao) ---\n" +
            "0: MAR = PC; rd; goto 1\n" +
            "1: PC = PC + 1; rd; goto 2\n" +
            "2: H = MBR; goto (MBR)\n" +
            "\n" +
            "// --- LODD (Carrega da Memoria) ---\n" +
            "10: MAR = MBR; rd; goto 11\n" +
            "11: rd; goto 12\n" +
            "12: AC = MDR; goto 0\n" +
            "\n" +
            "// --- STOD (Salva na Memoria) ---\n" +
            "20: MAR = MBR; goto 21\n" +
            "21: MDR = AC; wr; goto 22\n" +
            "22: wr; goto 0\n" +
            "\n" +
            "// --- ADDD (Soma) ---\n" +
            "30: MAR = MBR; rd; goto 31\n" +
            "31: rd; goto 32\n" +
            "32: H = AC; goto 33\n" +
            "33: AC = H + MDR; goto 0\n" +
            "\n" +
            "// --- SUBD (Subtrai) ---\n" +
            "40: MAR = MBR; rd; goto 41\n" +
            "41: rd; goto 42\n" +
            "42: H = AC; goto 43\n" +
            "43: AC = H + INV(MDR); goto 44\n" +
            "44: AC = AC + 1; goto 0\n" +
            "\n" +
            "// --- JPOS (Pula se AC >= 0) ---\n" +
            "50: H = AC; if N then goto 0 else goto 51\n" +
            "51: PC = MBR; goto 0\n" +
            "\n" +
            "// --- JUMP (Pula Incondicional) ---\n" +
            "60: PC = MBR; goto 0\n" +
            "\n" +
            "// --- JZER (Pula se AC == 0) ---\n" +
            "70: H = AC; if Z then goto 71 else goto 0\n" +
            "71: PC = MBR; goto 0";
    }

    private void step() {
        String instr = simulador.codigoFonteMicro[simulador.reg.MPC];
        if(instr == null) instr = "Microinstrucao Vazia";
        areaLog.setText("MPC: " + simulador.reg.MPC + " | " + instr + "\nCache: " + simulador.cache.ultimoStatus);
        simulador.step();
        painelVisual.repaint();
    }

    private void toggleRun() {
        if(isRunning) stopRunning(); else startRunning();
    }

    private void startRunning() {
        isRunning = true;
        btnRunStop.setText("Parar");
        btnStep.setEnabled(false);
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> SwingUtilities.invokeLater(this::step), 
            0, sliderSpeed.getValue(), TimeUnit.MILLISECONDS);
    }

    private void stopRunning() {
        if(scheduler != null) scheduler.shutdownNow();
        isRunning = false;
        btnRunStop.setText("Executar");
        btnStep.setEnabled(true);
    }

    public static void main(String[] args) { SwingUtilities.invokeLater(Main::new); }
}