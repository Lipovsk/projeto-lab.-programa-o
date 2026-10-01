package br.edu.unit.chemest.io;
import br.edu.unit.chemest.model.OrbitalResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
/** Adaptação do analisador original: leitura em fluxo e último par Alpha completo. */
public final class GaussianOutputParser {
    public OrbitalResult parse(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("O arquivo informado não existe ou não é um arquivo regular.");
        double homoFinal = Double.NaN, lumoFinal = Double.NaN, homoTemporario = Double.NaN;
        boolean aguardandoLumo = false;
        try (BufferedReader leitor = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1)) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.contains("Alpha  occ. eigenvalues --")) {
                    homoTemporario = number(linha, true);
                    aguardandoLumo = true;
                } else if (linha.contains("Alpha virt. eigenvalues --") && aguardandoLumo) {
                    double lumo = number(linha, false);
                    homoFinal = homoTemporario;
                    lumoFinal = lumo;
                    aguardandoLumo = false;
                }
            }
        }
        if (Double.isNaN(homoFinal) || Double.isNaN(lumoFinal))
            throw new GaussianParseException("Não foi possível extrair HOMO e LUMO: bloco Alpha ausente ou incompleto.");
        return new OrbitalResult(homoFinal, lumoFinal);
    }
    private static double number(String line, boolean last) {
        String[] values = line.substring(line.indexOf("--") + 2).trim().split("\\s+");
        try {
            double n = Double.parseDouble(values[last ? values.length - 1 : 0].replace('D', 'E'));
            if (!Double.isFinite(n)) throw new NumberFormatException();
            return n;
        } catch (NumberFormatException e) {
            throw new GaussianParseException("O arquivo contém valores de energia inválidos.");
        }
    }
}
