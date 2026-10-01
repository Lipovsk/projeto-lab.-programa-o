package br.edu.unit.chemest;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;
class GaussianOutputParserTest {
    @TempDir Path dir;
    final GaussianOutputParser parser = new GaussianOutputParser();
    Path sample(String text) throws Exception { return Files.writeString(dir.resolve("sample.out"), text); }
    String pair(double h, double l) { return "Alpha  occ. eigenvalues -- " + h + "\nAlpha virt. eigenvalues -- " + l + "\n"; }
    @Test void knownValuesAndConversion() throws Exception {
        var r = parser.parse(Path.of(getClass().getResource("/gaussian_sample.out").toURI()));
        assertEquals(-0.25, r.homoHartree(), 1e-8); assertEquals(-0.05, r.lumoHartree(), 1e-8);
        assertEquals(0.2, r.gapHartree(), 1e-8); assertEquals(5.4422772491976, r.gapEv(), 1e-8);
    }
    @Test void lastCompletePair() throws Exception {
        var r = parser.parse(sample(pair(-0.5,0.2)+pair(-0.4,0.1)));
        assertEquals(-0.4,r.homoHartree(),1e-8); assertEquals(0.1,r.lumoHartree(),1e-8);
    }
    @Test void incompleteTrailingPairKeepsPrevious() throws Exception {
        assertEquals(-0.5,parser.parse(sample(pair(-0.5,0.2)+"Alpha  occ. eigenvalues -- -0.1\n")).homoHartree(),1e-8);
    }
    @Test void multilineOriginalRegression() throws Exception {
        var r=parser.parse(sample("Alpha  occ. eigenvalues -- -1.0 -0.5\nAlpha  occ. eigenvalues -- -0.3 -0.263040\nAlpha virt. eigenvalues -- 0.075790 0.2\nAlpha virt. eigenvalues -- 0.4 0.8\n"));
        assertEquals(-0.263040,r.homoHartree(),1e-8); assertEquals(0.075790,r.lumoHartree(),1e-8);
        assertEquals(0.338830,r.gapHartree(),1e-8); assertEquals(9.220034002,r.gapEv(),1e-8);
    }
    @Test void noVirtual() throws Exception { var p=sample("Alpha  occ. eigenvalues -- -0.5"); assertThrows(GaussianParseException.class,()->parser.parse(p)); }
    @Test void absent() throws Exception { var p=sample("Beta occ. eigenvalues -- -0.5"); assertThrows(GaussianParseException.class,()->parser.parse(p)); }
    @Test void invalidNumber() throws Exception { var p=sample("Alpha  occ. eigenvalues -- invalid\nAlpha virt. eigenvalues -- 0.1"); assertThrows(GaussianParseException.class,()->parser.parse(p)); }
    @Test void missingFile() { assertThrows(IOException.class,()->parser.parse(dir.resolve("missing"))); }
    @Test void rejectsNonFinite() { assertThrows(IllegalArgumentException.class,()->new OrbitalResult(Double.NaN,0)); }
    @Test void zeroProcessors() { assertThrows(IllegalArgumentException.class,()->new CalculationConfig(0,"2GB","HF","STO-3G","SP",0,1)); }
}
