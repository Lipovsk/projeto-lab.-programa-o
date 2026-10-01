package br.edu.unit.chemest.service;
import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
public final class BatchGenerationService {
    public record Status(String id, String smiles, boolean success, String message) {}
    public List<Status> generate(List<CsvMoleculeReader.Row> rows,Path folder,CalculationConfig config) {
        List<Status> statuses=new ArrayList<>();
        for(var row:rows) {
            if(!row.valid()) { statuses.add(new Status(row.id(),row.smiles(),false,row.error())); continue; }
            try {
                Path destination=folder.resolve(row.id()+".gjf");
                if(Files.exists(destination)) throw new IOException("GJF já existe; escolha outra pasta ou renomeie o arquivo.");
                var atoms=new FileCoordinateProvider().read(folder.resolve(row.id()+".xyz"));
                new GaussianInputWriter().write(new MoleculeRecord(row.id(),row.id(),row.smiles(),atoms),config,destination);
                statuses.add(new Status(row.id(),row.smiles(),true,"Gerado: "+destination.getFileName()));
            } catch(IOException | IllegalArgumentException e) {
                statuses.add(new Status(row.id(),row.smiles(),false,"Falha: "+e.getMessage()));
            }
        }
        return List.copyOf(statuses);
    }
}
