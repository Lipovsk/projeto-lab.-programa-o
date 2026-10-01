package br.edu.unit.chemest.model;
import java.nio.file.Path;
import java.util.Objects;
public record AnalysisRecord(Path source, OrbitalResult result) {
    public AnalysisRecord { Objects.requireNonNull(source); Objects.requireNonNull(result); }
}
