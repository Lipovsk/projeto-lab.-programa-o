import br.edu.unit.chemest.io.*;
import br.edu.unit.chemest.model.*;
import br.edu.unit.chemest.service.*;
import br.edu.unit.chemest.report.HtmlReportWriter;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** Runner de entrega, fora de src/main. Executar a partir da raiz do projeto. */
public class GenerateDelivery {
    static void near(double actual, double expected) {
        if (!Double.isFinite(actual) || Math.abs(actual-expected)>1e-8) throw new AssertionError(actual+" != "+expected);
    }
    static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    public static void main(String[] args) throws Exception {
        Path inputs=Path.of("data/exemplos"), output=Path.of("resultados");
        Files.createDirectories(output);
        var smiles=new SmilesService();
        if(smiles.parse("CCO").getAtomCount()!=3)throw new AssertionError("SMILES CCO");
        var atoms=new FileCoordinateProvider().read(inputs.resolve("etanol.xyz"));
        if(atoms.size()!=9 || atoms.stream().filter(a->a.symbol().equals("H")).count()!=6)throw new AssertionError("Geometria");
        var config=CalculationConfig.defaults();
        new GaussianInputWriter().write(new MoleculeRecord("1","Etanol - demonstracao","CCO",atoms),config,output.resolve("exemplo.gjf"));
        Path bcf=output.resolve("exemplo.bcf");
        new GaussianBatchWriter().write(output,bcf);
        // Único pós-processamento: caminhos locais absolutos tornam-se relativos à pasta do BCF.
        String raw=Files.readString(bcf);
        String portable=raw.replace(output.toAbsolutePath().normalize().toString()+java.io.File.separator,"");
        if(portable.contains(output.toAbsolutePath().toString()))throw new AssertionError("Caminho pessoal no BCF");
        Files.writeString(bcf,portable);
        Path sample=inputs.resolve("gaussian_sample.out");
        var r=new GaussianOutputParser().parse(sample);
        near(r.homoHartree(),-.25); near(r.lumoHartree(),-.05); near(r.gapHartree(),.2); near(r.gapEv(),5.4422772491976);
        var records=List.of(new AnalysisRecord(sample,r));
        new ResultCsvWriter().write(records,output.resolve("resultados.csv"));
        new HtmlReportWriter().write(records,config,
            "AMOSTRA SINTÉTICA PARA TESTES. Não é resultado de execução real do Gaussian. " +
            "As energias sintéticas não foram calculadas para a geometria de etanol fornecida. " +
            "Configuração padrão usada somente como referência para geração do GJF.", output.resolve("relatorio.html"));
        var rows=new CsvMoleculeReader().read(inputs.resolve("moleculas.csv"));
        if(rows.size()!=2 || rows.stream().anyMatch(row->!row.valid()))throw new AssertionError("CSV");
        Path batch=Files.createTempDirectory(Path.of("target"),"delivery-batch-");
        Files.copy(inputs.resolve("1.xyz"),batch.resolve("1.xyz"));
        var statuses=new BatchGenerationService().generate(rows,batch,config);
        if(!statuses.get(0).success() || statuses.get(1).success())throw new AssertionError("Lote: esperado 1 sucesso e 1 falha");
        String evidence=String.format(Locale.US,
            "Gerador: docs/validation/GenerateDelivery.java%nJava: %s%nAmostra: %s%nSHA256 amostra: %s%n"+
            "XYZ aceito pelo FileCoordinateProvider: 9 atomos (C2H6O)%nCSV: 2 registros validos%nLote: 1 GJF gerado (etanol); 1 falha esperada (2.xyz ausente)%n"+
            "HOMO=%.12f%nLUMO=%.12f%ngapHartree=%.12f%ngapEv=%.15f%n"+
            "BCF: gerado por GaussianBatchWriter; caminhos relativizados pelo runner; execucao Gaussian NAO VALIDADA%n",
            System.getProperty("java.version"),sample,hash(sample),r.homoHartree(),r.lumoHartree(),r.gapHartree(),r.gapEv());
        Files.writeString(Path.of("docs/validation/geracao.txt"),evidence);
        System.out.print(evidence);
    }
}
