package br.edu.unit.chemest.service;
import br.edu.unit.chemest.io.GaussianOutputParser;
import br.edu.unit.chemest.model.AnalysisRecord;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
public final class AnalysisService {
    public record Outcome(Path source, AnalysisRecord record, String error) {}
    public List<Outcome> analyze(List<Path> paths) {
        List<Outcome> outcomes=new ArrayList<>();
        for(Path path:paths) {
            try { outcomes.add(new Outcome(path,new AnalysisRecord(path,new GaussianOutputParser().parse(path)),"")); }
            catch(IOException | IllegalArgumentException e) { outcomes.add(new Outcome(path,null,"Falha ao analisar "+path.getFileName()+": "+e.getMessage())); }
        }
        return List.copyOf(outcomes);
    }
}
