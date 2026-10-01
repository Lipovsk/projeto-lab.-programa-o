package br.edu.unit.chemest.model;
public record CalculationConfig(int processors, String memory, String method, String basis, String job, int charge, int multiplicity) {
    public CalculationConfig {
        if (processors < 1) throw new IllegalArgumentException("Processadores deve ser maior ou igual a 1.");
        if (multiplicity < 1) throw new IllegalArgumentException("Multiplicidade deve ser maior ou igual a 1.");
        for (String value : new String[]{memory, method, basis, job})
            if (value == null || value.isBlank() || value.contains("\n") || value.contains("\r"))
                throw new IllegalArgumentException("Informe memória, método, base e tarefa em uma linha.");
    }
    public static CalculationConfig defaults() { return new CalculationConfig(2, "2GB", "B3LYP", "6-31G(d)", "Opt", 0, 1); }
}
