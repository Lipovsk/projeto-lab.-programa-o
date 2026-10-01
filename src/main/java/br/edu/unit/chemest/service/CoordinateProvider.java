package br.edu.unit.chemest.service;
import br.edu.unit.chemest.model.Atom3D;
import java.nio.file.Path;
import java.io.IOException;
import java.util.List;
public interface CoordinateProvider { List<Atom3D> read(Path source) throws IOException; }
