package org.example;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Regression tests runnable with the JDK, without dependencies. */
public class HomoLumoAnalyzerTest {
    public static void main(String[] args) throws Exception {
        Path file = Files.createTempFile("orbitals-test-", ".out");
        try {
            Files.writeString(file, "Alpha  occ. eigenvalues -- -1.0 -0.5\n"
                    + "Alpha  occ. eigenvalues -- -0.3 -0.263040\n"
                    + "Alpha virt. eigenvalues -- 0.075790 0.2\n"
                    + "Alpha virt. eigenvalues -- 0.4 0.8\n", StandardCharsets.ISO_8859_1);
            Resultado result = HomoLumoAnalyzer.analisarArquivo(file.toString());
            near(result.homo(), -0.263040);
            near(result.lumo(), 0.075790);
            near(result.gapHartree(), 0.338830);
            near(result.gapEv(), 9.220034002);
            Files.writeString(file, "Alpha  occ. eigenvalues -- -0.5\nAlpha virt. eigenvalues -- 0.2\n"
                    + "Alpha  occ. eigenvalues -- -0.4\nAlpha virt. eigenvalues -- 0.1\n");
            near(HomoLumoAnalyzer.analisarArquivo(file.toString()).homo(), -0.4);
            near(HomoLumoAnalyzer.analisarArquivo(file.toString()).lumo(), 0.1);
            Files.writeString(file, "Sem orbitais\n");
            rejects(file);
            Files.writeString(file, "Alpha  occ. eigenvalues -- -0.5\n");
            rejects(file);
            Files.writeString(file, "Alpha  occ. eigenvalues -- invalido\nAlpha virt. eigenvalues -- 0.1\n");
            rejects(file);
            Files.delete(file);
            rejects(file);
            System.out.println("OK: energies, conversion, multiline, last pair, absent orbitals, incomplete pair, invalid number, missing file.");
        } finally {
            Files.deleteIfExists(file);
        }
    }
    private static void near(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-8) throw new AssertionError(actual + " != " + expected);
    }
    private static void rejects(Path file) throws Exception {
        try {
            HomoLumoAnalyzer.analisarArquivo(file.toString());
            throw new AssertionError("Expected rejection");
        } catch (IllegalArgumentException expected) { }
    }
}
