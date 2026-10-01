package br.edu.unit.chemest.io;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.CoordinateException;
import java.nio.file.*;
import java.io.*;
import java.util.Locale;
public final class GaussianInputWriter {
    public void write(MoleculeRecord molecule, CalculationConfig config, Path destination) throws IOException {
        if(molecule.atoms().isEmpty()) throw new CoordinateException("A molécula não possui coordenadas 3D.");
        try(BufferedWriter writer=Files.newBufferedWriter(destination)) {
            writer.write(String.format(Locale.US,"%%nprocshared=%d%n%%mem=%s%n# %s/%s %s%n%n%s | %s%n%n%d %d%n",
                config.processors(),config.memory(),config.method(),config.basis(),config.job(),
                molecule.name(),molecule.smiles(),config.charge(),config.multiplicity()));
            for(Atom3D atom:molecule.atoms())
                writer.write(String.format(Locale.US,"%s %.10f %.10f %.10f%n",atom.symbol(),atom.x(),atom.y(),atom.z()));
            writer.newLine();
        }
    }
}
