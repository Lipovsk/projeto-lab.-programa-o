package br.edu.unit.chemest.io;
import br.edu.unit.chemest.model.AnalysisRecord;
import org.apache.commons.csv.*;
import java.nio.file.*;
import java.io.*;
import java.util.List;
public final class ResultCsvWriter {
    public void write(List<AnalysisRecord> results,Path destination) throws IOException {
        SafeFileWriter.write(destination, true, writer -> {
            var csv=new CSVPrinter(writer,CSVFormat.DEFAULT.builder().setHeader("arquivo","HOMO","LUMO","gapHartree","gapEv").get());
            for(var row:results)csv.printRecord(row.source().toString(),row.result().homoHartree(),row.result().lumoHartree(),row.result().gapHartree(),row.result().gapEv());
            csv.flush(); // SafeFileWriter owns and closes the underlying writer.
        });
    }
}
