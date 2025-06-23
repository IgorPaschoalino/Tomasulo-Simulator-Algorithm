import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections; // Para Collections.unmodifiableList

// Esta classe conterá a lógica do seu simulador
public class TOMASSULLLERoriSimulator {

    private RegisterFile registerFile;
    private ReservationStation[] rsAdd;
    private ReservationStation[] rsStore;
    private ReservationStation[] rsMult;
    private ReservationStation[] rsBranch;

    private List<Instruction> allInstructions; // ROB simplificado
    private String cdbProducerTag = null;
    private Double cdbValue = null;

    private int bubbleCycles = 0;
    private int currentClock = 0;
    private int programCounter = 0; // Índice da próxima instrução a ser emitida

    // Latências das UFs (definidas na configuração da GUI)
    private int addSubLatency;
    private int loadLatency;
    private int storeLatency;
    private int multLatency;
    private int divLatency;
    private int branchLatency;

    // Construtor para ser chamado pela GUI
    public TOMASSULLLERoriSimulator(int addFuCount, int storeFuCount, int multFuCount, int branchFuCount,
                                   int addSubLatency, int loadLatency, int storeLatency, int multLatency, int divLatency, int branchLatency) {
        this.addSubLatency = addSubLatency;
        this.loadLatency = loadLatency;
        this.storeLatency = storeLatency;
        this.multLatency = multLatency;
        this.divLatency = divLatency;
        this.branchLatency = branchLatency;

        registerFile = new RegisterFile();

        rsAdd = new ReservationStation[addFuCount];
        for (int i = 0; i < addFuCount; i++) rsAdd[i] = new ReservationStation("RS_ADD_" + (i + 1));

        rsStore = new ReservationStation[storeFuCount];
        for (int i = 0; i < storeFuCount; i++) rsStore[i] = new ReservationStation("RS_STORE_" + (i + 1));

        rsMult = new ReservationStation[multFuCount];
        for (int i = 0; i < multFuCount; i++) rsMult[i] = new ReservationStation("RS_MULT_" + (i + 1));

        rsBranch = new ReservationStation[branchFuCount];
        for (int i = 0; i < branchFuCount; i++) rsBranch[i] = new ReservationStation("RS_BRANCH_" + (i + 1));

        allInstructions = new ArrayList<>();
        resetSimulationState(); // Garante um estado limpo
    }

    // Método para a GUI carregar as instruções
    public void setInstructions(List<Instruction> instructions) {
        this.allInstructions = instructions;
        resetSimulationState(); // Reseta o estado quando novas instruções são carregadas
    }

    private void resetSimulationState() {
        currentClock = 0;
        bubbleCycles = 0;
        programCounter = 0;
        cdbProducerTag = null;
        cdbValue = null;

        // Limpa todas as RSs
        for (ReservationStation rs : rsAdd) rs.free();
        for (ReservationStation rs : rsStore) rs.free();
        for (ReservationStation rs : rsMult) rs.free();
        for (ReservationStation rs : rsBranch) rs.free();

        // Reseta o Register File (você pode querer inicializar com valores padrão ou manter os últimos)
        // Para simplicidade, vamos re-inicializar
        registerFile = new RegisterFile();

        // Reseta o estado das instruções (importante para múltiplas simulações)
        for (Instruction instr : allInstructions) {
            instr.setIssueCycle(-1);
            instr.setStartExecCycle(-1);
            instr.setEndExecCycle(-1);
            instr.setWriteResultCycle(-1);
            instr.setCommitCycle(-1);
            instr.setCurrentLatency(instr.getOriginalLatency()); // Reseta a latência
            instr.setBranchTaken(false);
            instr.setBranchResolved(false);
            instr.setSquashed(false);
        }
    }

    // Método para avançar um ciclo (chamado pelo botão "Próximo Ciclo" da GUI)
    public void nextCycle() {
        if (isSimulationFinished()) {
            return;
        }
        currentClock++;
        // As fases na ordem correta
        commitInstructions();
        writeResultToCDB();
        executeInstructions();
        issueFromInstructionQueue();
    }

    // Método para executar até o fim (chamado pelo botão "Executar Tudo" da GUI)
    public void runToEnd() {
        while (!isSimulationFinished()) {
            nextCycle();
        }
    }

    // Verifica se a simulação terminou
    public boolean isSimulationFinished() {
        if (allInstructions.isEmpty()) return true; // Se não há instruções, a simulação está "pronta"
        for (Instruction instr : allInstructions) {
            if (!instr.isSquashed() && instr.getCommitCycle() == -1) {
                return false; // Ainda há instruções não comitadas e não descartadas
            }
        }
        return true; // Todas as instruções foram comitadas ou descartadas
    }

    // --- Métodos de Lógica (Adaptados do seu código original) ---

    private void issueFromInstructionQueue() {
        if (programCounter < allInstructions.size()) {
            Instruction instrToIssue = allInstructions.get(programCounter);
            
            if (instrToIssue.isSquashed() || instrToIssue.getIssueCycle() != -1) {
                programCounter++; // Já squashed ou emitida, avança
                return;
            }

            ReservationStation[] targetRsArray = null;
            if (instrToIssue.getOp() == Instruction.Op.ADD || instrToIssue.getOp() == Instruction.Op.SUB) {
                targetRsArray = rsAdd;
            } else if (instrToIssue.getOp() == Instruction.Op.LD || instrToIssue.getOp() == Instruction.Op.ST) {
                targetRsArray = rsStore;
            } else if (instrToIssue.getOp() == Instruction.Op.MUL || instrToIssue.getOp() == Instruction.Op.DIV) {
                targetRsArray = rsMult;
            } else if (instrToIssue.getOp() == Instruction.Op.BEQ || instrToIssue.getOp() == Instruction.Op.BNE) {
                targetRsArray = rsBranch;
            }

            if (targetRsArray != null) {
                boolean issued = false;
                for (ReservationStation rs : targetRsArray) {
                    if (!rs.isBusy()) {
                        String Qj = null; Double Vj = null;
                        String Qk = null; Double Vk = null;

                        // Resolve src1
                        if (!instrToIssue.getSrc1().equals("0")) {
                            RegisterFile.RegisterStatus src1Status = registerFile.getRegisterStatus(instrToIssue.getSrc1());
                            if (src1Status != null) {
                                if (src1Status.getProducerTag() != null) Qj = src1Status.getProducerTag();
                                else Vj = src1Status.getValue();
                            } else { try { Vj = Double.parseDouble(instrToIssue.getSrc1()); } catch (NumberFormatException e) { Vj = 0.0; } }
                        } else { Vj = 0.0; }

                        // Resolve src2
                        if (!instrToIssue.getSrc2().equals("0")) {
                            RegisterFile.RegisterStatus src2Status = registerFile.getRegisterStatus(instrToIssue.getSrc2());
                            if (src2Status != null) {
                                if (src2Status.getProducerTag() != null) Qk = src2Status.getProducerTag();
                                else Vk = src2Status.getValue();
                            } else { try { Vk = Double.parseDouble(instrToIssue.getSrc2()); } catch (NumberFormatException e) { Vk = 0.0; } }
                        } else { Vk = 0.0; }

                        rs.assign(instrToIssue, instrToIssue.getOp(), Qj, Vj, Qk, Vk);
                        instrToIssue.setIssueCycle(currentClock);

                        // Atualizar o Register File (RAT) para o registrador de destino
                        if (instrToIssue.getDest() != null && !instrToIssue.getDest().equals("0")) {
                            registerFile.updateRegisterStatus(instrToIssue.getDest(), null, rs.getName());
                        }
                        issued = true;
                        break;
                    }
                }
                if (issued) {
                    programCounter++;
                } else {
                    bubbleCycles++;
                }
            }
        }
    }

    private void executeInstructions() {
        // Copia as RSs para evitar ConcurrentModificationException se uma RS for liberada
        List<ReservationStation> allRS = new ArrayList<>();
        allRS.addAll(Arrays.asList(rsAdd));
        allRS.addAll(Arrays.asList(rsStore));
        allRS.addAll(Arrays.asList(rsMult));
        allRS.addAll(Arrays.asList(rsBranch));

        for (ReservationStation rs : allRS) {
            if (rs.isBusy()) {
                Instruction instr = rs.getInstruction();
                if (instr.isSquashed()) {
                    continue;
                }

                if (instr.getStartExecCycle() == -1 && rs.isReadyToExecute()) {
                    instr.setStartExecCycle(currentClock);
                } else if (instr.getStartExecCycle() != -1 && instr.getEndExecCycle() == -1) {
                    if (instr.getCurrentLatency() > 0) {
                        instr.setCurrentLatency(instr.getCurrentLatency() - 1);
                        if (instr.getCurrentLatency() == 0) {
                            instr.setEndExecCycle(currentClock);
                            double res = 0.0; // Placeholder para o resultado

                            // Lógica para branches - SEMPRE TOMA O SALTO nesta simulação
                            if ((rs.getOp() == Instruction.Op.BEQ || rs.getOp() == Instruction.Op.BNE) && !instr.isBranchResolved()) {
                                instr.setBranchTaken(true);
                                instr.setBranchResolved(true);
                                res = 1.0; // Simboliza branch tomado
                            }
                            rs.setResult(res);
                        }
                    }
                } else if (instr.getStartExecCycle() == -1 && !rs.isReadyToExecute()) {
                    bubbleCycles++;
                }
            }
        }
    }

    private void writeResultToCDB() {
        cdbProducerTag = null; // Limpa o CDB no início da fase
        cdbValue = null;

        List<ReservationStation> allRS = new ArrayList<>();
        allRS.addAll(Arrays.asList(rsAdd));
        allRS.addAll(Arrays.asList(rsStore));
        allRS.addAll(Arrays.asList(rsMult));
        allRS.addAll(Arrays.asList(rsBranch));

        // Prioridade de escrita no CDB: aqui, a primeira RS que terminou escreve.
        // Em um sistema real, haveria um arbiter.
        for (ReservationStation rs : allRS) {
            if (rs.isBusy() && rs.getInstruction().getEndExecCycle() != -1 && rs.getInstruction().getWriteResultCycle() == -1) {
                if (rs.getInstruction().isSquashed()) {
                    freeReservationStation(rs.getInstruction(), rsAdd);
                    freeReservationStation(rs.getInstruction(), rsStore);
                    freeReservationStation(rs.getInstruction(), rsMult);
                    freeReservationStation(rs.getInstruction(), rsBranch);
                    continue;
                }
                
                cdbProducerTag = rs.getName();
                cdbValue = rs.getResult();
                rs.getInstruction().setWriteResultCycle(currentClock);

                // Disparar atualizações para outras RSs e Register File
                updateReservationStationsFromCDB(cdbProducerTag, cdbValue);
                updateRegisterFileFromCDB(cdbProducerTag, cdbValue, rs.getInstruction().getDest());
                
                // Uma RS por ciclo publica no CDB para simplificação
                return; 
            }
        }
    }

    private void updateReservationStationsFromCDB(String producerTag, Double value) {
        List<ReservationStation> allRS = new ArrayList<>();
        allRS.addAll(Arrays.asList(rsAdd));
        allRS.addAll(Arrays.asList(rsStore));
        allRS.addAll(Arrays.asList(rsMult));
        allRS.addAll(Arrays.asList(rsBranch));

        for (ReservationStation rs : allRS) {
            if (rs.isBusy()) {
                if (rs.getQj() != null && rs.getQj().equals(producerTag)) {
                    rs.setVj(value);
                }
                if (rs.getQk() != null && rs.getQk().equals(producerTag)) {
                    rs.setVk(value);
                }
            }
        }
    }

    private void updateRegisterFileFromCDB(String producerTag, Double value, String destRegister) {
        if (destRegister != null && !destRegister.equals("0")) {
            RegisterFile.RegisterStatus status = registerFile.getRegisterStatus(destRegister);
            if (status != null && status.getProducerTag() != null && status.getProducerTag().equals(producerTag)) {
                registerFile.updateRegisterStatus(destRegister, value, null);
            }
        }
    }

    private void commitInstructions() {
        int instrIndexToCommit = -1;
        for (int i = 0; i < allInstructions.size(); i++) {
            if (!allInstructions.get(i).isSquashed() && allInstructions.get(i).getCommitCycle() == -1) {
                instrIndexToCommit = i;
                break;
            }
        }

        if (instrIndexToCommit != -1) {
            Instruction instrToCommit = allInstructions.get(instrIndexToCommit);

            if (instrToCommit.getWriteResultCycle() != -1) {
                instrToCommit.setCommitCycle(currentClock);
                
                // --- Lógica de SQUASHING para BRANCHES ---
                if ((instrToCommit.getOp() == Instruction.Op.BEQ || instrToCommit.getOp() == Instruction.Op.BNE) && instrToCommit.isBranchTaken()) {
                    int targetInstructionIndex = -1;
                    for (int i = 0; i < allInstructions.size(); i++) {
                        if (allInstructions.get(i).getId() == instrToCommit.getBranchTargetId()) {
                            targetInstructionIndex = i;
                            break;
                        }
                    }

                    if (targetInstructionIndex != -1) {
                        for (int i = instrIndexToCommit + 1; i < allInstructions.size(); i++) {
                            Instruction futureInstr = allInstructions.get(i);
                            if (futureInstr.getCommitCycle() == -1 && !futureInstr.isSquashed()) {
                                futureInstr.setSquashed(true);
                                freeReservationStation(futureInstr, rsAdd);
                                freeReservationStation(futureInstr, rsStore);
                                freeReservationStation(futureInstr, rsMult);
                                freeReservationStation(futureInstr, rsBranch);
                            }
                        }
                        programCounter = targetInstructionIndex;
                    }
                }
                
                // Libera a RS da instrução comitada
                freeReservationStation(instrToCommit, rsAdd);
                freeReservationStation(instrToCommit, rsStore);
                freeReservationStation(instrToCommit, rsMult);
                freeReservationStation(instrToCommit, rsBranch);
                
                // Para instruções STORE, o valor é efetivamente "escrito" na memória no commit
                if (instrToCommit.getOp() == Instruction.Op.ST) {
                    ReservationStation storeRs = findReservationStation(instrToCommit, rsStore);
                    if (storeRs != null) {
                        // Poderia simular a escrita em uma memória aqui
                    }
                }
            }
        }
    }

    private void freeReservationStation(Instruction committedInstruction, ReservationStation[] rsArray) {
        for (ReservationStation rs : rsArray) {
            if (rs.isBusy() && rs.getInstruction() == committedInstruction) {
                rs.free();
                return;
            }
        }
    }

    private ReservationStation findReservationStation(Instruction instruction, ReservationStation[] rsArray) {
        for (ReservationStation rs : rsArray) {
            if (rs.isBusy() && rs.getInstruction() == instruction) {
                return rs;
            }
        }
        return null;
    }

    // --- Getters para a GUI ---
    public int getCurrentClock() { return currentClock; }
    public int getBubbleCycles() { return bubbleCycles; }
    public List<Instruction> getAllInstructions() { return Collections.unmodifiableList(allInstructions); } // Retorna uma lista imutável
    public ReservationStation[] getRsAdd() { return rsAdd; }
    public ReservationStation[] getRsStore() { return rsStore; }
    public ReservationStation[] getRsMult() { return rsMult; }
    public ReservationStation[] getRsBranch() { return rsBranch; }
    public RegisterFile getRegisterFile() { return registerFile; }

    public double calculateIPC() {
        int committedCount = 0;
        for (Instruction instr : allInstructions) {
            if (instr.getCommitCycle() != -1 && !instr.isSquashed()) {
                committedCount++;
            }
        }
        if (currentClock == 0) return 0.0;
        return (double) committedCount / currentClock;
    }
}