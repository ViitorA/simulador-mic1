
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main extends JFrame {
    private Mic1Simulador simulador;
    private PainelDatapath painelVisual;

    private JTabbedPane abas_editores;
    private JPanel painel_mic1, painel_mac1, painel_direito;
    private JPanel controles;
    private JTextArea editor_mic1, editor_mac1;
    private JTextArea visualizador_logs;
    private JButton btnStep, btnRunStop, btnReset;
    private JButton btn_montarMac1, btn_montarMic1;
    private JSlider sliderSpeed;
    private JSplitPane splitMain;

    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;
    

    private final Font EDITOR_FONT = new Font("Monospaced", Font.PLAIN, 14);
    private final Color EDITOR_FOREGROUND = new Color(0, 0, 100);

    public Main() {
        super("Simulador MIC-1/MAC-1");
        
        simulador = new Mic1Simulador();
        painelVisual = new PainelDatapath(simulador);
        
        splitMain = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        this.add(splitMain, BorderLayout.CENTER);

        // ÁREA DOS EDITORES DE CÓDIGO
        mostrarEditores();

        // --- Controles de Execução do Código ---
        mostrarControles();

        // --- Área que mostra os logs ---
        mostrarLogs();
        
        splitMain.setRightComponent(painel_direito);
        splitMain.setDividerLocation(400);

        compilarMicrocodigo(); 

        btn_montarMic1.addActionListener(e -> compilarMicrocodigo());
        
        btn_montarMac1.addActionListener(e -> {
            try {
                int[] obj = Montador.montar(editor_mac1.getText());
                simulador.carregarPrograma(obj);
                visualizador_logs.append("\nPrograma montado com sucesso.");
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
            visualizador_logs.setText("Sistema Zerado. Recarregue o programa.");
            painelVisual.repaint();
        });

        this.setSize(1200, 750);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void compilarMicrocodigo() {
        try {
            simulador.compilarMicrocodigo(editor_mic1.getText());
            visualizador_logs.setText("Microcodigo compilado e carregado.");
            simulador.reg.MPC = 0;
        } catch (Exception e) { e.printStackTrace(); }
    }

    // --- PROGRAMAS-PADRÃO OU DE EXEMPLO QUE CARREGAM QUANDO O SIMULADOR ABRE ---

    // MAC-1
    private String getMacroprogramaPadrao() {
        return
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
    }

    // MIC-1
    private String getMicrocodigoPadrao() {
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

    // --- MOSTRA AS ABAS DOS EDITORES DE CÓDIGOS ---
    private void mostrarEditores() {
        abas_editores = new JTabbedPane();
        
        // MIC-1
        painel_mic1 = new JPanel(new BorderLayout());
        
        editor_mic1 = new JTextArea(getMicrocodigoPadrao());
        editor_mic1.setFont(EDITOR_FONT);
        editor_mic1.setForeground(EDITOR_FOREGROUND);
        painel_mic1.add(new JScrollPane(editor_mic1), BorderLayout.CENTER);
        
        btn_montarMic1 = new JButton("Montar Microcodigo");
        painel_mic1.add(btn_montarMic1, BorderLayout.SOUTH);

        abas_editores.addTab("MIC-1", null, painel_mic1);

        // MAC-1
        painel_mac1 = new JPanel(new BorderLayout());
        
        editor_mac1 = new JTextArea(getMacroprogramaPadrao());
        editor_mac1.setFont(EDITOR_FONT);
        editor_mac1.setForeground(EDITOR_FOREGROUND);
        painel_mac1.add(new JScrollPane(editor_mac1), BorderLayout.CENTER);
        
        btn_montarMac1 = new JButton("Montar Macroprograma");
        painel_mac1.add(btn_montarMac1, BorderLayout.SOUTH);
        
        abas_editores.addTab("MAC-1", null, painel_mac1);
        
        splitMain.setLeftComponent(abas_editores);

        painel_direito = new JPanel(new BorderLayout());
        painel_direito.add(painelVisual, BorderLayout.CENTER);
    }

    // --- MOSTRA CONTROLES DE EXECUÇÃO DE CÓDIGO ---
    private void mostrarControles() {
        controles = new JPanel(new FlowLayout());
        btnReset = new JButton("Reiniciar");
        btnStep = new JButton("Passo");
        btnRunStop = new JButton("Executar");
        
        sliderSpeed = new JSlider(100, 1500, 500);
        controles.add(new JLabel("Atraso (ms):"));
        controles.add(sliderSpeed);
        controles.add(btnReset);
        controles.add(btnStep);
        controles.add(btnRunStop);

        painel_direito.add(controles, BorderLayout.NORTH);
    }

    private void mostrarLogs() {
        visualizador_logs = new JTextArea(4, 40);
        visualizador_logs.setEditable(false);
        visualizador_logs.setFont(EDITOR_FONT);
        
        painel_direito.add(new JScrollPane(visualizador_logs), BorderLayout.SOUTH);
    }

    private void step() {
        String instr = simulador.codigoFonteMicro[simulador.reg.MPC];
        if(instr == null) instr = "Microinstrucao Vazia";
        visualizador_logs.setText("MPC: " + simulador.reg.MPC + " | " + instr + "\nCache: " + simulador.cache.ultimoStatus);
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