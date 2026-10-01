package br.edu.unit.chemest;

import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class IoRegressionTest {
    @TempDir Path dir;
    @Test void acceptsBomInCsvHeaderAndXyzCount() throws Exception {
        Path csv=Files.writeString(dir.resolve("m.csv"),"\uFEFFNUM,SMILES\n1,CCO\n");
        assertTrue(new CsvMoleculeReader().read(csv).getFirst().valid());
        Path xyz=Files.writeString(dir.resolve("1.xyz"),"\uFEFF1\ncomment\nC 0 0 0\n");
        assertEquals(1,new FileCoordinateProvider().read(xyz).size());
    }
    @Test void rejectsDuplicateHeaders() throws Exception {
        Path csv=Files.writeString(dir.resolve("m.csv"),"NUM,SMILES,NUM\n1,CCO,2\n");
        assertThrows(IOException.class,()->new CsvMoleculeReader().read(csv));
    }
    @Test void rejectsReservedWindowsNamesAndKeepsOtherRows() throws Exception {
        Path csv=Files.writeString(dir.resolve("m.csv"),"NUM,SMILES\nCON,CCO\nlpt1,CCO\nCOM9,CCO\n1,CCO\n");
        var rows=new CsvMoleculeReader().read(csv);
        assertEquals(4,rows.size());assertEquals(1,rows.stream().filter(CsvMoleculeReader.Row::valid).count());
        assertTrue(rows.getFirst().error().contains("Windows"));
    }
    @Test void malformedCsvFailsWithIoException() throws Exception {
        Path csv=Files.writeString(dir.resolve("m.csv"),"NUM,SMILES\n1,\"CCO\n");
        assertThrows(IOException.class,()->new CsvMoleculeReader().read(csv));
    }
    @Test void shortRowDoesNotAbortValidFollowingRow() throws Exception {
        Path csv=Files.writeString(dir.resolve("m.csv"),"NUM,SMILES\n1\n2,CCO\n");
        var rows=new CsvMoleculeReader().read(csv);
        assertFalse(rows.getFirst().valid());assertTrue(rows.getLast().valid());
    }
    @Test void failedWritePreservesOldFileAndCleansTemporaryFile() throws Exception {
        Path target=Files.writeString(dir.resolve("result.csv"),"original");
        assertThrows(IOException.class,()->SafeFileWriter.write(target,true,w->{w.write("partial");throw new IOException("simulated failure");}));
        assertEquals("original",Files.readString(target));
        try(var files=Files.list(dir)){assertEquals(1,files.count());}
    }
    @Test void failedNewWriteLeavesNoOutput() throws Exception {
        Path target=dir.resolve("new.gjf");
        assertThrows(IOException.class,()->SafeFileWriter.write(target,false,w->{w.write("partial");throw new IOException("simulated failure");}));
        assertFalse(Files.exists(target));
        try(var files=Files.list(dir)){assertEquals(0,files.count());}
    }
    @Test void publicationDoesNotOverwriteFileCreatedDuringGeneration() throws Exception {
        Path target=dir.resolve("new.gjf");
        assertThrows(FileAlreadyExistsException.class,()->SafeFileWriter.write(target,false,w->{w.write("new content");Files.writeString(target,"concurrent content");}));
        assertEquals("concurrent content",Files.readString(target));
    }
    @Test void inputWriterNewModeKeepsExistingGjf() throws Exception {
        Path target=Files.writeString(dir.resolve("1.gjf"),"original");
        var molecule=new MoleculeRecord("1","hydrogen","[H]",List.of(new Atom3D("H",0,0,0)));
        assertThrows(FileAlreadyExistsException.class,()->new GaussianInputWriter().writeNew(molecule,CalculationConfig.defaults(),target));
        assertEquals("original",Files.readString(target));
    }
    @Test void bcfCannotOverwriteInputOrCalculationOutput() throws Exception {
        Path input=Files.writeString(dir.resolve("a.gjf"),"input");
        Path output=Files.writeString(dir.resolve("a.out"),"output");
        var writer=new GaussianBatchWriter();
        assertThrows(IOException.class,()->writer.write(dir,input));
        assertThrows(IOException.class,()->writer.write(dir,output));
        assertEquals("input",Files.readString(input));assertEquals("output",Files.readString(output));
    }
    @Test void emptyBcfFolderKeepsExistingDestination() throws Exception {
        Path target=Files.writeString(dir.resolve("batch.bcf"),"original");
        assertThrows(IOException.class,()->new GaussianBatchWriter().write(dir,target));
        assertEquals("original",Files.readString(target));
    }
    @Test void nonexistentBatchDirectoryHasClearError() {
        var error=assertThrows(IllegalArgumentException.class,()->new BatchGenerationService().generate(List.of(),dir.resolve("missing"),CalculationConfig.defaults()));
        assertTrue(error.getMessage().contains("pasta existente"));
    }
    @Test void xyzRejectsSecondGeometryAndUnknownElement() throws Exception {
        Path target=Files.writeString(dir.resolve("a.xyz"),"1\ncomment\nH 0 0 0\n1\nsecond\nH 1 0 0\n");
        assertThrows(CoordinateException.class,()->new FileCoordinateProvider().read(target));
        Files.writeString(target,"1\ncomment\nXx 0 0 0\n");
        assertThrows(CoordinateException.class,()->new FileCoordinateProvider().read(target));
    }
}
