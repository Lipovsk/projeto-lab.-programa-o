package br.edu.unit.chemest;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.report.*;
import org.apache.commons.csv.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class ReportTest {
    @TempDir Path dir;
    @Test void exportCsvAndHtml() throws Exception {
        var rows=List.of(new AnalysisRecord(Path.of("sample,out"),new OrbitalResult(-0.25,-0.05)));
        Path csv=dir.resolve("results.csv"),html=dir.resolve("report.html");
        new ResultCsvWriter().write(rows,csv);
        try(var reader=Files.newBufferedReader(csv);var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get().parse(reader)) {
            var row=parser.getRecords().getFirst();assertEquals("sample,out",row.get("arquivo"));
            assertEquals(0.2,Double.parseDouble(row.get("gapHartree")),1e-8);
            assertEquals(5.4422772491976,Double.parseDouble(row.get("gapEv")),1e-8);
        }
        new HtmlReportWriter().write(rows,CalculationConfig.defaults(),"<script>alert('x')</script>&",html);
        String text=Files.readString(html);
        assertTrue(text.contains("ChemEST Java"));assertTrue(text.contains("5.4422772492"));
        assertTrue(text.contains("&lt;script&gt;"));assertFalse(text.contains("<script>"));
        assertTrue(text.contains("não foi extraída"));assertTrue(text.contains("sample,out"));
    }
    @Test void reportWriteFailure() {
        assertThrows(ReportWriteException.class,()->new HtmlReportWriter().write(List.of(),CalculationConfig.defaults(),"",dir.resolve("missing/report.html")));
    }
}
