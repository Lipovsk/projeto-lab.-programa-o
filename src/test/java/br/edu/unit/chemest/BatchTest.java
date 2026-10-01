package br.edu.unit.chemest;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.service.*;
import br.edu.unit.chemest.model.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class BatchTest {
    @TempDir Path dir;
    @Test void individualValidationAndBatch() throws Exception {
        var csv=Files.writeString(dir.resolve("m.csv"),"SMILES,NUM\nCCO,1\nC1(,2\nCCO,1\nCCO,../bad\nCCO,3\n");
        var rows=new CsvMoleculeReader().read(csv);
        assertEquals(5,rows.size()); assertEquals(2,rows.stream().filter(CsvMoleculeReader.Row::valid).count());
        Files.writeString(dir.resolve("1.xyz"),"1\nfixture\nC 0 0 1\n");
        var statuses=new BatchGenerationService().generate(rows,dir,CalculationConfig.defaults());
        assertEquals(1,statuses.stream().filter(BatchGenerationService.Status::success).count());
        assertTrue(Files.exists(dir.resolve("1.gjf"))); assertFalse(Files.exists(dir.resolve("3.gjf")));
        assertFalse(new BatchGenerationService().generate(rows,dir,CalculationConfig.defaults()).getFirst().success());
    }
    @Test void bcfSortedOnlyGjf() throws Exception {
        Files.writeString(dir.resolve("b.gjf"),""); Files.writeString(dir.resolve("a.gjf"),"");
        Files.writeString(dir.resolve("x.txt"),""); Files.createDirectory(dir.resolve("directory.gjf"));
        var out=dir.resolve("batch.bcf"); new GaussianBatchWriter().write(dir,out);
        String text=Files.readString(out);
        assertTrue(text.startsWith("!\n!batch file\n!start=1\n!\n"));
        assertTrue(text.indexOf("a.gjf")<text.indexOf("b.gjf")); assertFalse(text.contains("x.txt")); assertFalse(text.contains("directory.gjf"));
        assertTrue(text.contains(dir.resolve("a.out").toString()));
    }
    @Test void missingHeader() throws Exception {
        var csv=Files.writeString(dir.resolve("m.csv"),"id,SMILES\n1,CCO");
        assertThrows(java.io.IOException.class,()->new CsvMoleculeReader().read(csv));
    }
}
