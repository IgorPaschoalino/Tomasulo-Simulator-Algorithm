import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Importar as classes do seu simulador
// import com.yourpackage.Instruction; // ajuste o pacote
// import com.yourpackage.ReservationStation; // ajuste o pacote
// import com.yourpackage.RegisterFile; // ajuste o pacote
// import com.yourpackage.TOMASSULLLERoriSimulator; // ajuste o pacote

public class TOMASSULLLERoriGUI extends JFrame {

    private TOMASSULLLERoriSimulator simulator; // Instância do seu simulador

    // --- Painel Esquerdo: Configurações e Entrada de Instruções ---
    private JPanel leftPanel;
    private JTextField numInstructionsField;
    private JButton loadInstructionsButton;
    private JPanel instructionInputPanel; // Painel dinâmico para entrada de instruções

    // Campos de input para unidades funcionais e latências
    private JTextField fuAdd, fuStore, fuMult, fuBranch;
    private JTextField latAdd, latLoad, latStore, latMult, latDiv, latBranch;
    private JButton setupSimulatorButton; // Botão para configurar o simulador

    // --- Painel Direito: Status da Simulação ---
    private JPanel rightPanel;
    private JLabel clockLabel;
    private JButton nextCycleButton;
    private JButton runToEndButton;
    private JScrollPane instructionStatusScrollPane;
    private JTable instructionStatusTable;
    private DefaultTableModel instructionStatusTableModel;

    private JScrollPane rsStatusScrollPane;
    private JTable rsStatusTable;
    private DefaultTableModel rsStatusTableModel;

    private JScrollPane registerFileScrollPane;
    private JTable registerFileTable;
    private DefaultTableModel registerFileTableModel;

    // Métricas
    private JLabel metricsLabel;

    public TOMASSULLLERoriGUI() {
        setTitle("Simulador do Algoritmo de Tomasulo");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout()); // Layout principal da janela

        initComponents(); // Inicializa todos os componentes
        addListeners();   // Adiciona os listeners aos botões
        updateUI(false);  // Desativa botões de simulação inicialmente
    }

    private void initComponents() {
        // --- Painel Esquerdo ---
        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Configurações e Entrada"));
        add(leftPanel, BorderLayout.WEST);

        // Input para quantidade de Unidades Funcionais
        JPanel fuPanel = new JPanel(new GridLayout(4, 2));
        fuPanel.setBorder(BorderFactory.createTitledBorder("Unidades Funcionais (Qtd)"));
        fuAdd = new JTextField("1"); fuPanel.add(new JLabel("ADD/SUB:")); fuPanel.add(fuAdd);
        fuStore = new JTextField("1"); fuPanel.add(new JLabel("LOAD/STORE:")); fuPanel.add(fuStore);
        fuMult = new JTextField("1"); fuPanel.add(new JLabel("MUL/DIV:")); fuPanel.add(fuMult);
        fuBranch = new JTextField("1"); fuPanel.add(new JLabel("BRANCH:")); fuPanel.add(fuBranch);
        leftPanel.add(fuPanel);

        // Input para Latências
        JPanel latPanel = new JPanel(new GridLayout(6, 2));
        latPanel.setBorder(BorderFactory.createTitledBorder("Latências (Ciclos)"));
        latAdd = new JTextField("2"); latPanel.add(new JLabel("ADD/SUB:")); latPanel.add(latAdd);
        latLoad = new JTextField("6"); latPanel.add(new JLabel("LOAD:")); latPanel.add(latLoad);
        latStore = new JTextField("6"); latPanel.add(new JLabel("STORE:")); latPanel.add(latStore);
        latMult = new JTextField("3"); latPanel.add(new JLabel("MUL:")); latPanel.add(latMult);
        latDiv = new JTextField("3"); latPanel.add(new JLabel("DIV:")); latPanel.add(latDiv);
        latBranch = new JTextField("4"); latPanel.add(new JLabel("BRANCH:")); latPanel.add(latBranch);
        leftPanel.add(latPanel);

        setupSimulatorButton = new JButton("Configurar Simulador");
        leftPanel.add(setupSimulatorButton);

        // Input para número de instruções
        JPanel numInstrPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        numInstrPanel.add(new JLabel("Número de Instruções:"));
        numInstructionsField = new JTextField(5);
        numInstrPanel.add(numInstructionsField);
        loadInstructionsButton = new JButton("OK");
        numInstrPanel.add(loadInstructionsButton);
        leftPanel.add(numInstrPanel);

        // Painel de entrada de instruções (será preenchido dinamicamente)
        instructionInputPanel = new JPanel();
        instructionInputPanel.setLayout(new BoxLayout(instructionInputPanel, BoxLayout.Y_AXIS));
        instructionInputPanel.setBorder(BorderFactory.createTitledBorder("Definir Instruções"));
        JScrollPane instructionInputScrollPane = new JScrollPane(instructionInputPanel);
        instructionInputScrollPane.setPreferredSize(new Dimension(300, 300)); // Ajuste de tamanho
        leftPanel.add(instructionInputScrollPane);


        // --- Painel Direito ---
        rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        add(rightPanel, BorderLayout.CENTER);

        // Controles do Clock
        JPanel clockControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        clockLabel = new JLabel("Clock: 0");
        clockControlPanel.add(clockLabel);
        nextCycleButton = new JButton("Próximo Ciclo");
        clockControlPanel.add(nextCycleButton);
        runToEndButton = new JButton("Executar Tudo");
        clockControlPanel.add(runToEndButton);
        rightPanel.add(clockControlPanel);

        // Tabela de Status das Instruções
        instructionStatusTableModel = new DefaultTableModel(new Object[]{"ID", "Op", "Dest", "Src1", "Src2", "Issue", "Exec Start", "Exec End", "Write Result", "Commit", "Squashed"}, 0);
        instructionStatusTable = new JTable(instructionStatusTableModel);
        instructionStatusScrollPane = new JScrollPane(instructionStatusTable);
        instructionStatusScrollPane.setBorder(BorderFactory.createTitledBorder("Status das Instruções"));
        rightPanel.add(instructionStatusScrollPane);

        // Tabela de Estações de Reserva
        rsStatusTableModel = new DefaultTableModel(new Object[]{"Nome", "Busy", "Op", "Vj", "Vk", "Qj", "Qk"}, 0);
        rsStatusTable = new JTable(rsStatusTableModel);
        rsStatusScrollPane = new JScrollPane(rsStatusTable);
        rsStatusScrollPane.setBorder(BorderFactory.createTitledBorder("Estações de Reserva"));
        rightPanel.add(rsStatusScrollPane);

        // Tabela de Register File
        registerFileTableModel = new DefaultTableModel(new Object[]{"Registrador", "Valor", "Produtor"}, 0);
        registerFileTable = new JTable(registerFileTableModel);
        registerFileScrollPane = new JScrollPane(registerFileTable);
        registerFileScrollPane.setBorder(BorderFactory.createTitledBorder("Register File"));
        rightPanel.add(registerFileScrollPane);
        
        // Métricas
        metricsLabel = new JLabel("Métricas: IPC: N/A, Ciclos Bolha: 0");
        rightPanel.add(metricsLabel);
    }

    private void addListeners() {
        setupSimulatorButton.addActionListener(e -> {
            try {
                int add = Integer.parseInt(fuAdd.getText());
                int store = Integer.parseInt(fuStore.getText());
                int mult = Integer.parseInt(fuMult.getText());
                int branch = Integer.parseInt(fuBranch.getText());

                int latAdd = Integer.parseInt(this.latAdd.getText());
                int latLoad = Integer.parseInt(this.latLoad.getText());
                int latStore = Integer.parseInt(this.latStore.getText());
                int latMult = Integer.parseInt(this.latMult.getText());
                int latDiv = Integer.parseInt(this.latDiv.getText());
                int latBranch = Integer.parseInt(this.latBranch.getText());

                // Passa as latências para o simulador
                simulator = new TOMASSULLLERoriSimulator(add, store, mult, branch, 
                                                            latAdd, latLoad, latStore, latMult, latDiv, latBranch);
                JOptionPane.showMessageDialog(this, "Simulador configurado com sucesso! Agora defina as instruções.");
                updateUI(false); // Mantém botões de simulação desativados até as instruções serem carregadas
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Por favor, insira números válidos para quantidades e latências.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            }
        });

        loadInstructionsButton.addActionListener(e -> {
            try {
                if (simulator == null) {
                    JOptionPane.showMessageDialog(this, "Por favor, configure o simulador primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int numInstructions = Integer.parseInt(numInstructionsField.getText());
                if (numInstructions <= 0) {
                    JOptionPane.showMessageDialog(this, "Número de instruções deve ser positivo.", "Erro", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                buildInstructionInputForm(numInstructions);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Por favor, insira um número válido de instruções.", "Erro de Entrada", JOptionPane.ERROR_MESSAGE);
            }
        });

        nextCycleButton.addActionListener(e -> {
            if (simulator != null) {
                simulator.nextCycle();
                updateTables();
                updateMetrics();
                if (simulator.isSimulationFinished()) {
                    JOptionPane.showMessageDialog(this, "Simulação Concluída!", "Fim", JOptionPane.INFORMATION_MESSAGE);
                    updateUI(false); // Desativa botões de simulação
                }
            }
        });

        runToEndButton.addActionListener(e -> {
            if (simulator != null) {
                simulator.runToEnd();
                updateTables();
                updateMetrics();
                JOptionPane.showMessageDialog(this, "Simulação Concluída!", "Fim", JOptionPane.INFORMATION_MESSAGE);
                updateUI(false); // Desativa botões de simulação
            }
        });
    }

    private void buildInstructionInputForm(int numInstructions) {
        instructionInputPanel.removeAll(); // Limpa painel anterior
        List<JTextField[]> instructionFields = new ArrayList<>(); // Para armazenar os campos de input

        String[] ops = {"ADD", "SUB", "LD", "ST", "BEQ", "BNE", "MUL", "DIV"}; // Ops MIPS simplificadas
        JComboBox<String> opComboBox;

        for (int i = 0; i < numInstructions; i++) {
            JPanel instrRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            instrRow.add(new JLabel("Instr " + i + ": "));
            opComboBox = new JComboBox<>(ops);
            JTextField dest = new JTextField(5);
            JTextField src1 = new JTextField(5);
            JTextField src2 = new JTextField(5);

            instrRow.add(opComboBox);
            instrRow.add(dest);
            instrRow.add(src1);
            instrRow.add(src2);
            
            instructionInputPanel.add(instrRow);
            instructionFields.add(new JTextField[]{dest, src1, src2}); // Armazena os campos para fácil acesso
        }

        JButton submitInstructionsButton = new JButton("Carregar Instruções no Simulador");
        submitInstructionsButton.addActionListener(e -> {
            List<Instruction> instructions = new ArrayList<>();
            for (int i = 0; i < numInstructions; i++) {
                JPanel instrRow = (JPanel) instructionInputPanel.getComponent(i);
                JComboBox<String> opCB = (JComboBox<String>) instrRow.getComponent(1); // O ComboBox é o 2º componente
                JTextField destField = (JTextField) instrRow.getComponent(2);
                JTextField src1Field = (JTextField) instrRow.getComponent(3);
                JTextField src2Field = (JTextField) instrRow.getComponent(4);

                Instruction.Op op = Instruction.Op.valueOf((String) opCB.getSelectedItem());
                String dest = destField.getText().trim().toUpperCase();
                String src1 = src1Field.getText().trim().toUpperCase();
                String src2 = src2Field.getText().trim().toUpperCase();

                int latency = 0;
                switch(op) { // Defina a latência baseada na sua configuração
                    case ADD: case SUB: latency = Integer.parseInt(latAdd.getText()); break;
                    case LD: latency = Integer.parseInt(latLoad.getText()); break;
                    case ST: latency = Integer.parseInt(latStore.getText()); break;
                    case MUL: latency = Integer.parseInt(latMult.getText()); break;
                    case DIV: latency = Integer.parseInt(latDiv.getText()); break;
                    case BEQ: case BNE: latency = Integer.parseInt(latBranch.getText()); break;
                }

                Instruction instr;
                if (op == Instruction.Op.BEQ || op == Instruction.Op.BNE) {
                    // Para branches, dest é o TARGET_ID, não um registrador.
                    // src1 e src2 são registradores de comparação.
                    try {
                        int targetId = Integer.parseInt(dest); // dest é o TARGET_ID
                        instr = new Instruction(i, op, "0", src1, src2, latency);
                        instr.setBranchTargetId(targetId);
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "ID do alvo do branch inválido para Instrução " + i + ". Use um número inteiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                        return; // Não carrega as instruções se houver erro
                    }
                } else {
                    instr = new Instruction(i, op, dest, src1, src2, latency);
                }
                instructions.add(instr);
            }
            simulator.setInstructions(instructions);
            updateTables();
            updateUI(true); // Ativa botões de simulação
            JOptionPane.showMessageDialog(this, "Instruções carregadas no simulador!");
        });
        instructionInputPanel.add(submitInstructionsButton);
        instructionInputPanel.revalidate();
        instructionInputPanel.repaint();
    }

    private void updateTables() {
        // Atualiza a tabela de Status das Instruções
        instructionStatusTableModel.setRowCount(0); // Limpa a tabela
        for (Instruction instr : simulator.getAllInstructions()) {
            instructionStatusTableModel.addRow(new Object[]{
                instr.getId(),
                instr.getOp(),
                instr.getDest(),
                instr.getSrc1(),
                instr.getSrc2(),
                instr.getIssueCycle() != -1 ? instr.getIssueCycle() : "N/A",
                instr.getStartExecCycle() != -1 ? instr.getStartExecCycle() : "N/A",
                instr.getEndExecCycle() != -1 ? instr.getEndExecCycle() : "N/A",
                instr.getWriteResultCycle() != -1 ? instr.getWriteResultCycle() : "N/A",
                instr.getCommitCycle() != -1 ? instr.getCommitCycle() : "N/A",
                instr.isSquashed() ? "SIM" : "NÃO"
            });
        }

        // Atualiza a tabela de Estações de Reserva
        rsStatusTableModel.setRowCount(0);
        addRsToTable(simulator.getRsAdd());
        addRsToTable(simulator.getRsStore());
        addRsToTable(simulator.getRsMult());
        addRsToTable(simulator.getRsBranch());

        // Atualiza a tabela de Register File
        registerFileTableModel.setRowCount(0);
        for (Map.Entry<String, RegisterFile.RegisterStatus> entry : simulator.getRegisterFile().getRegisters().entrySet()) {
            RegisterFile.RegisterStatus status = entry.getValue();
            String producerInfo = (status.getProducerTag() != null) ? status.getProducerTag() : "N/A";
            registerFileTableModel.addRow(new Object[]{
                entry.getKey(),
                status.getValue(),
                producerInfo
            });
        }
        
        // Atualiza o clock
        clockLabel.setText("Clock: " + simulator.getCurrentClock());
    }

    private void addRsToTable(ReservationStation[] rsArray) {
        for (ReservationStation rs : rsArray) {
            rsStatusTableModel.addRow(new Object[]{
                rs.getName(),
                rs.isBusy() ? "SIM" : "NÃO",
                rs.getOp() != null ? rs.getOp().name() : "N/A",
                rs.getVj() != null ? String.format("%.2f", rs.getVj()) : "N/A",
                rs.getVk() != null ? String.format("%.2f", rs.getVk()) : "N/A",
                rs.getQj() != null ? rs.getQj() : "N/A",
                rs.getQk() != null ? rs.getQk() : "N/A"
            });
        }
    }

    private void updateMetrics() {
        double ipc = simulator.calculateIPC();
        int bubbleCycles = simulator.getBubbleCycles();
        metricsLabel.setText(String.format("Métricas: IPC: %.2f, Ciclos Bolha: %d", ipc, bubbleCycles));
    }

    private void updateUI(boolean simulationReady) {
        nextCycleButton.setEnabled(simulationReady);
        runToEndButton.setEnabled(simulationReady);
        // Desabilitar campos de config depois que o simulador for configurado
        fuAdd.setEnabled(!simulationReady);
        fuStore.setEnabled(!simulationReady);
        fuMult.setEnabled(!simulationReady);
        fuBranch.setEnabled(!simulationReady);
        latAdd.setEnabled(!simulationReady);
        latLoad.setEnabled(!simulationReady);
        latStore.setEnabled(!simulationReady);
        latMult.setEnabled(!simulationReady);
        latDiv.setEnabled(!simulationReady);
        latBranch.setEnabled(!simulationReady);
        setupSimulatorButton.setEnabled(!simulationReady);
        numInstructionsField.setEnabled(!simulationReady); // Desabilita o campo de qtd de instruções depois que as instruções são carregadas
        loadInstructionsButton.setEnabled(!simulationReady); // E o botão OK
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new TOMASSULLLERoriGUI().setVisible(true);
        });
    }
}