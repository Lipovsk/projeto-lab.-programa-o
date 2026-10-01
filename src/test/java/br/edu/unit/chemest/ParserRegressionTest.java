package br.edu.unit.chemest;
import br.edu.unit.chemest.io.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ParserRegressionTest {
    @TempDir Path dir;
    Path sample(String text) throws Exception { return Files.writeString(dir.resolve("sample.out"),text); }
    @Test void acceptsVariableWhitespaceAndFortranExponents() throws Exception {
        var result=new GaussianOutputParser().parse(sample("  Alpha\tocc. eigenvalues  -- -1.0D+00 -2.5d-01\n Alpha   virt. eigenvalues -- -5.0D-02 0.1\n"));
        assertEquals(-0.25,result.homoHartree());assertEquals(-0.05,result.lumoHartree());
    }
    @Test void doesNotPairOrbitalsAcrossUnrelatedBlock() throws Exception {
        Path file=sample("Alpha  occ. eigenvalues -- -0.5\nNormal termination\nAlpha virt. eigenvalues -- 0.2\n");
        assertThrows(GaussianParseException.class,()->new GaussianOutputParser().parse(file));
    }
    @Test void unrelatedTrailingVirtualDoesNotReplaceLastCompletePair() throws Exception {
        var result=new GaussianOutputParser().parse(sample("Alpha  occ. eigenvalues -- -0.5\nAlpha virt. eigenvalues -- 0.2\nAlpha  occ. eigenvalues -- -0.1\nEntering new job\nAlpha virt. eigenvalues -- 0.9\n"));
        assertEquals(-0.5,result.homoHartree());assertEquals(0.2,result.lumoHartree());
    }
    @Test void invalidInteriorEnergyIsRejected() throws Exception {
        Path file=sample("Alpha  occ. eigenvalues -- -1.0 invalid -0.5\nAlpha virt. eigenvalues -- 0.2\n");
        assertThrows(GaussianParseException.class,()->new GaussianOutputParser().parse(file));
    }
    @Test void invalidEnergyAfterLumoIsRejected() throws Exception {
        Path file=sample("Alpha  occ. eigenvalues -- -0.5\nAlpha virt. eigenvalues -- 0.2 NaN\n");
        assertThrows(GaussianParseException.class,()->new GaussianOutputParser().parse(file));
    }
}
