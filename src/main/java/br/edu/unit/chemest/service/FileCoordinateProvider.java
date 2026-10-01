package br.edu.unit.chemest.service;
import br.edu.unit.chemest.model.Atom3D;
import java.nio.file.*;
import java.io.*;
import java.util.*;
/** XYZ explícito em angstroms. Geometrias planares são válidas; nunca inventa z. */
public final class FileCoordinateProvider implements CoordinateProvider {
    @Override public List<Atom3D> read(Path source) throws IOException {
        if (!source.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".xyz"))
            throw new CoordinateException("Selecione um arquivo XYZ com x, y e z explícitos, em angstroms.");
        try (BufferedReader reader=Files.newBufferedReader(source)) {
            String first=reader.readLine();
            if(first!=null && first.startsWith("\uFEFF"))first=first.substring(1);
            int count;
            try { count=Integer.parseInt(first == null ? "" : first.trim()); }
            catch (NumberFormatException e) { throw new CoordinateException("XYZ inválido: informe a quantidade de átomos na primeira linha."); }
            if (count<1 || reader.readLine()==null) throw new CoordinateException("XYZ incompleto: faltam átomos ou linha de comentário.");
            List<Atom3D> atoms=new ArrayList<>();
            for(int i=0;i<count;i++) {
                String line=reader.readLine();
                if(line==null) throw new CoordinateException("XYZ incompleto: faltam coordenadas.");
                String[] fields=line.trim().split("\\s+");
                if(fields.length!=4) throw new CoordinateException("Cada átomo deve conter símbolo, x, y e z.");
                try { atoms.add(new Atom3D(fields[0],Double.parseDouble(fields[1]),Double.parseDouble(fields[2]),Double.parseDouble(fields[3]))); }
                catch(IllegalArgumentException e) { throw new CoordinateException("Coordenadas inválidas no átomo "+(i+1)+": "+e.getMessage()); }
            }
            String line;
            while((line=reader.readLine())!=null) if(!line.isBlank())
                throw new CoordinateException("Importe somente uma geometria XYZ por arquivo.");
            return List.copyOf(atoms);
        }
    }
}
