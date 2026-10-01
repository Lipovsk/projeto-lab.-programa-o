package br.edu.unit.chemest.io;
import br.edu.unit.chemest.model.OrbitalResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.regex.Pattern;
/** Leitura em fluxo: último bloco Alpha ocupado/virtual completo, sem cruzar outros blocos. */
public final class GaussianOutputParser {
    private static final Pattern ORBITALS = Pattern.compile("^\\s*Alpha\\s+(occ\\.|virt\\.)\\s+eigenvalues\\s*--\\s*(.*?)\\s*$");
    public OrbitalResult parse(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("O arquivo informado não existe ou não é um arquivo regular.");
        double homoFinal = Double.NaN, lumoFinal = Double.NaN, homoTemporario = Double.NaN;
        boolean aguardandoLumo = false;
        try (BufferedReader leitor = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                var orbital = ORBITALS.matcher(linha);
                if (!orbital.matches()) { aguardandoLumo = false; continue; }
                if (orbital.group(1).equals("occ.")) {
                    homoTemporario = number(orbital.group(2), true);
                    aguardandoLumo = true;
                } else if (aguardandoLumo) {
                    lumoFinal = number(orbital.group(2), false);
                    homoFinal = homoTemporario;
                    aguardandoLumo = false;
                }
            }
        }
        if (Double.isNaN(homoFinal) || Double.isNaN(lumoFinal))
            throw new GaussianParseException("Não foi possível extrair HOMO e LUMO: bloco Alpha ausente ou incompleto.");
        return new OrbitalResult(homoFinal, lumoFinal);
    }
    private static double number(String energies, boolean last) {
        String[] values = energies.split("\\s+");
        double selected = Double.NaN;
        try {
            for(int i=0;i<values.length;i++) {
                double n = Double.parseDouble(values[i].replace('D', 'E').replace('d', 'E'));
                if (!Double.isFinite(n)) throw new NumberFormatException();
                if(i == (last ? values.length-1 : 0)) selected = n;
            }
            return selected;
        } catch (NumberFormatException e) {
            throw new GaussianParseException("O arquivo contém valores de energia inválidos.");
        }
    }
}
