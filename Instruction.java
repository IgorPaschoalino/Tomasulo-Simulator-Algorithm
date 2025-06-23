
public class Instruction {
    public enum Op {
        ADD, SUB, LD, ST, BEQ, BNE, MUL, DIV
    }

    private int id; // ID da instrução na ordem de entrada
    private Op op;
    private String dest;
    private String src1;
    private String src2;
    private int originalLatency;
    private int currentLatency;

    private int issueCycle = -1;
    private int startExecCycle = -1;
    private int endExecCycle = -1;
    private int writeResultCycle = -1;
    private int commitCycle = -1;

    // Novos campos para Branches
    private int branchTargetId = -1;    // ID da instrução para onde o branch salta
    private boolean branchTaken = false; // Indica se o branch foi tomado (true para a simulação)
    private boolean branchResolved = false; // Indica se o branch já foi resolvido/executado uma vez
    private boolean squashed = false;   // Indica se a instrução foi descartada (squashed)

    public Instruction(int id, Op op, String dest, String src1, String src2, int latency) {
        this.id = id;
        this.op = op;
        this.dest = dest;
        this.src1 = src1;
        this.src2 = src2;
        this.originalLatency = latency;
        this.currentLatency = latency;
    }

    // Getters
    public int getId() { return id; }
    public Op getOp() { return op; }
    public String getDest() { return dest; }
    public String getSrc1() { return src1; }
    public String getSrc2() { return src2; }
    public int getOriginalLatency() { return originalLatency; }
    public int getCurrentLatency() { return currentLatency; }
    public int getIssueCycle() { return issueCycle; }
    public int getStartExecCycle() { return startExecCycle; }
    public int getEndExecCycle() { return endExecCycle; }
    public int getWriteResultCycle() { return writeResultCycle; }
    public int getCommitCycle() { return commitCycle; }

    // Getters para Branches
    public int getBranchTargetId() { return branchTargetId; }
    public boolean isBranchTaken() { return branchTaken; }
    public boolean isBranchResolved() { return branchResolved; }
    public boolean isSquashed() { return squashed; }


    // Setters
    public void setCurrentLatency(int currentLatency) { this.currentLatency = currentLatency; }
    public void setIssueCycle(int issueCycle) { this.issueCycle = issueCycle; }
    public void setStartExecCycle(int startExecCycle) { this.startExecCycle = startExecCycle; }
    public void setEndExecCycle(int endExecCycle) { this.endExecCycle = endExecCycle; }
    public void setWriteResultCycle(int writeResultCycle) { this.writeResultCycle = writeResultCycle; }
    public void setCommitCycle(int commitCycle) { this.commitCycle = commitCycle; }

    // Setters para Branches
    public void setBranchTargetId(int branchTargetId) { this.branchTargetId = branchTargetId; }
    public void setBranchTaken(boolean branchTaken) { this.branchTaken = branchTaken; }
    public void setBranchResolved(boolean branchResolved) { this.branchResolved = branchResolved; }
    public void setSquashed(boolean squashed) { this.squashed = squashed; }


    @Override
    public String toString() {
        String baseString = "Instr " + id + ": " + op + " " + dest + ", " + src1 + ", " + src2 + 
               " | Issue: " + (issueCycle != -1 ? issueCycle : "N/A") + 
               ", Start Exec: " + (startExecCycle != -1 ? startExecCycle : "N/A") + 
               ", End Exec: " + (endExecCycle != -1 ? endExecCycle : "N/A") + 
               ", Write Result: " + (writeResultCycle != -1 ? writeResultCycle : "N/A") +
               ", Commit: " + (commitCycle != -1 ? commitCycle : "N/A") + 
               " | Latency Restante: " + currentLatency;
        if (squashed) {
            return baseString + " (DESCARTADA)";
        }
        return baseString;
    }
}