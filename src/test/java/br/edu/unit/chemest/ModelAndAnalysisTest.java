package br.edu.unit.chemest;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.AnalysisService;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ModelAndAnalysisTest {
    @TempDir Path dir;
    @Test void configValidation() {
        assertThrows(IllegalArgumentException.class,()->new CalculationConfig(1,"2GB","HF","STO-3G","SP",0,0));
        assertThrows(IllegalArgumentException.class,()->new CalculationConfig(1,"","HF","STO-3G","SP",0,1));
        assertThrows(IllegalArgumentException.class,()->new CalculationConfig(1,"2GB","HF\nINJECT","STO-3G","SP",0,1));
    }
    @Test void immutableAtoms() {
        var atoms=new ArrayList<Atom3D>();atoms.add(new Atom3D("H",0,0,0));
        var molecule=new MoleculeRecord("1","H","[H]",atoms);atoms.clear();
        assertEquals(1,molecule.atoms().size());
        assertThrows(UnsupportedOperationException.class,()->molecule.atoms().clear());
    }
    @Test void batchAnalysisContinuesAfterInvalidFile() throws Exception {
        var good=Files.writeString(dir.resolve("good.out"),"Alpha  occ. eigenvalues -- -0.25\nAlpha virt. eigenvalues -- -0.05\n");
        var outcomes=new AnalysisService().analyze(List.of(dir.resolve("missing.out"),good));
        assertNull(outcomes.getFirst().record());assertFalse(outcomes.getFirst().error().isBlank());
        assertEquals(0.2,outcomes.getLast().record().result().gapHartree(),1e-8);
    }
}
