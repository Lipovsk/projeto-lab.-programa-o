package br.edu.unit.chemest.io;
import br.edu.unit.chemest.model.MoleculeRecord;
import br.edu.unit.chemest.service.SmilesService;
import org.apache.commons.csv.*;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public final class CsvMoleculeReader {
    public record Row(long line, String id, String smiles, MoleculeRecord molecule, String error) {
        public boolean valid() { return molecule != null; }
    }
    public List<Row> read(Path file) throws IOException {
        List<Row> rows=new ArrayList<>();
        Set<String> ids=new HashSet<>();
        try(Reader reader=Files.newBufferedReader(file);
            CSVParser parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).get().parse(reader)) {
            if(!parser.getHeaderMap().containsKey("NUM") || !parser.getHeaderMap().containsKey("SMILES"))
                throw new IOException("O CSV deve conter as colunas NUM e SMILES.");
            for(CSVRecord record:parser) {
                String id="", smiles="";
                try {
                    if(!record.isConsistent()) throw new IllegalArgumentException("Quantidade de colunas inconsistente.");
                    id=record.get("NUM"); smiles=record.get("SMILES");
                    if(!id.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("NUM deve conter apenas letras, números, _ ou -.");
                    if(!ids.add(id.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("NUM duplicado.");
                    new SmilesService().parse(smiles);
                    rows.add(new Row(record.getRecordNumber(),id,smiles,new MoleculeRecord(id,id,smiles,List.of()),""));
                } catch(IllegalArgumentException e) { rows.add(new Row(record.getRecordNumber(),id,smiles,null,e.getMessage())); }
            }
        } catch(UncheckedIOException e) { throw new IOException("CSV malformado: confira aspas e separadores.",e); }
        return List.copyOf(rows);
    }
}
