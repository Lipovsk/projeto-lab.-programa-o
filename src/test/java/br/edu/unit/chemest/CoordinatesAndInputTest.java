package br.edu.unit.chemest;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.service.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class CoordinatesAndInputTest {
    @TempDir Path dir;
    @Test void noGeometryBlocked() {
        assertThrows(CoordinateException.class,()->new GaussianInputWriter().write(new MoleculeRecord("1","ethanol","CCO",List.of()),CalculationConfig.defaults(),dir.resolve("a.gjf")));
        assertFalse(Files.exists(dir.resolve("a.gjf")));
    }
    @Test void xyzToGjfUsesDecimalPoint() throws Exception {
        var p=Files.writeString(dir.resolve("a.xyz"),"2\nexplicit geometry\nH 0 0 0\nH 0 0 0.74\n");
        var atoms=new FileCoordinateProvider().read(p);
        var output=dir.resolve("a.gjf");
        Locale old=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("pt-BR"));
            new GaussianInputWriter().write(new MoleculeRecord("1","hydrogen","[H][H]",atoms),CalculationConfig.defaults(),output);
        } finally { Locale.setDefault(old); }
        String text=Files.readString(output);
        assertTrue(text.contains("0.7400000000")); assertTrue(text.contains("%nprocshared=2")); assertTrue(text.contains("0 1"));
        assertFalse(text.contains("0,740"));
    }
    @Test void missingZRejected() throws Exception {
        var p=Files.writeString(dir.resolve("a.xyz"),"1\ncomment\nH 0 0\n");
        assertThrows(CoordinateException.class,()->new FileCoordinateProvider().read(p));
    }
    @Test void nonFiniteRejected() throws Exception {
        var p=Files.writeString(dir.resolve("a.xyz"),"1\ncomment\nH NaN 0 1\n");
        assertThrows(CoordinateException.class,()->new FileCoordinateProvider().read(p));
    }
    @Test void truncatedRejected() throws Exception {
        var p=Files.writeString(dir.resolve("a.xyz"),"2\ncomment\nH 0 0 1\n");
        assertThrows(CoordinateException.class,()->new FileCoordinateProvider().read(p));
    }
    @Test void planarExplicitCoordinatesAccepted() throws Exception {
        var p=Files.writeString(dir.resolve("a.xyz"),"2\ncomment\nH 0 0 0\nH 0.74 0 0\n");
        assertEquals(2,new FileCoordinateProvider().read(p).size());
    }
}
