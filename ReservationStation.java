
public class ReservationStation {
    private String name;
    private boolean busy;
    private Instruction.Op op; // Operação
    private String Qj, Qk;     // Tags das RS produtoras dos operandos
    private Double Vj, Vk;     // Valores dos operandos (se disponíveis)
    private Double result;     // Resultado da execução
    private Instruction instruction; // Referência à instrução que está nesta RS

    public ReservationStation(String name) {
        this.name = name;
        this.busy = false;
        free(); // Inicializa em estado livre
    }

    // Atribui uma instrução a esta Reservation Station
    public void assign(Instruction instr, Instruction.Op op, String Qj, Double Vj, String Qk, Double Vk) {
        this.busy = true;
        this.instruction = instr;
        this.op = op;
        this.Qj = Qj;
        this.Vj = Vj;
        this.Qk = Qk;
        this.Vk = Vk;
        this.result = null; // Limpa o resultado anterior
    }

    // Libera esta Reservation Station
    public void free() {
        this.busy = false;
        this.instruction = null;
        this.op = null;
        this.Qj = null;
        this.Vj = null;
        this.Qk = null;
        this.Vk = null;
        this.result = null;
    }

    // Verifica se a RS está pronta para executar (operandos disponíveis)
    public boolean isReadyToExecute() {
        // Se Qj e Qk são null, significa que os valores Vj e Vk estão disponíveis.
        return Qj == null && Qk == null; 
    }

    // Getters
    public String getName() { return name; }
    public boolean isBusy() { return busy; }
    public Instruction.Op getOp() { return op; }
    public String getQj() { return Qj; }
    public Double getVj() { return Vj; }
    public String getQk() { return Qk; }
    public Double getVk() { return Vk; }
    public Double getResult() { return result; }
    public Instruction getInstruction() { return instruction; }

    // Setters
    public void setVj(Double vj) { Vj = vj; Qj = null; } // Valor recebido, Qj limpo
    public void setVk(Double vk) { Vk = vk; Qk = null; } // Valor recebido, Qk limpo
    public void setResult(Double result) { this.result = result; }
    public void setQj(String Qj) { this.Qj = Qj; } // Usado para redefinir Qj se a instrução produtora é descartada
    public void setQk(String Qk) { this.Qk = Qk; } // Usado para redefinir Qk se a instrução produtora é descartada


    @Override
    public String toString() {
        return String.format("%s: %s | Op: %s | Qj: %s, Vj: %.2f | Qk: %s, Vk: %.2f | Result: %.2f | Instr ID: %s",
            name, 
            busy ? "Busy" : "Free", 
            op != null ? op.name() : "N/A",
            Qj != null ? Qj : "N/A", 
            Vj != null ? Vj : 0.0,
            Qk != null ? Qk : "N/A", 
            Vk != null ? Vk : 0.0,
            result != null ? result : 0.0,
            instruction != null ? instruction.getId() : "N/A"
        );
    }
}