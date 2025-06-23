

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RegisterFile {
    private Map<String, RegisterStatus> registers;

    public RegisterFile() {
        registers = new HashMap<>();
        // Inicializa alguns registradores com valores padrão (ou "prontos")
        // Ex: R0-R10 (Para suportar o limite de 10)
        for (int i = 0; i <= 10; i++) { // Ajustado para ir até R10
            registers.put("R" + i, new RegisterStatus(0.0, null)); // Valor 0.0, sem produtor
        }
    }

    public RegisterStatus getRegisterStatus(String registerName) {
        return registers.get(registerName);
    }

    public void updateRegisterStatus(String registerName, Double value, String producerTag) {
        if (!registers.containsKey(registerName)) {
            // Adiciona se não existe (útil se você usar outros registradores)
            registers.put(registerName, new RegisterStatus(value, producerTag));
        } else {
            RegisterStatus status = registers.get(registerName);
            status.setValue(value);
            status.setProducerTag(producerTag);
        }
    }

    // Getter para a GUI acessar os dados do Register File
    public Map<String, RegisterStatus> getRegisters() {
        return Collections.unmodifiableMap(registers); // Retorna uma visão imutável
    }


    public static class RegisterStatus {
        private Double value;
        private String producerTag; // Nome da RS que está produzindo o valor para este registrador

        public RegisterStatus(Double value, String producerTag) {
            this.value = value;
            this.producerTag = producerTag;
        }

        public Double getValue() { return value; }
        public void setValue(Double value) { this.value = value; }
        public String getProducerTag() { return producerTag; }
        public void setProducerTag(String producerTag) { this.producerTag = producerTag; }
    }
}