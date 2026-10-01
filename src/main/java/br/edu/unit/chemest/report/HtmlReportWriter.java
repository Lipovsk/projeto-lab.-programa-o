package br.edu.unit.chemest.report;
import br.edu.unit.chemest.model.*;
import java.nio.file.*;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
public final class HtmlReportWriter {
    public void write(List<AnalysisRecord> results,CalculationConfig config,String observations,Path destination) throws ReportWriteException {
        StringBuilder html=new StringBuilder("""
            <!doctype html><html lang="pt-BR"><meta charset="UTF-8"><meta name="viewport" content="width=device-width">
            <title>ChemEST Java — relatório</title>
            <style>body{font:16px system-ui;margin:3rem;color:#172839}table{border-collapse:collapse;width:100%}th,td{padding:.7rem;border:1px solid #bdcbd6;text-align:left;overflow-wrap:anywhere}th{background:#e4eef4}pre{white-space:pre-wrap}h1{color:#146b78}</style>
            <h1>ChemEST Java</h1>
            """);
        html.append("<p>Data: ").append(escape(ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))).append("</p>");
        html.append("<h2>Configuração atual da interface</h2><p>Referência para geração de entradas; não foi extraída dos arquivos OUT e não comprova a configuração dos cálculos analisados.</p><pre>")
            .append(escape(config.toString())).append("</pre>");
        html.append("<table><thead><tr><th>Origem do arquivo</th><th>HOMO (Hartree)</th><th>LUMO (Hartree)</th><th>Gap (Hartree)</th><th>Gap (eV)</th></tr></thead><tbody>");
        for(var row:results) {
            var r=row.result();
            html.append("<tr><td>").append(escape(row.source().toString())).append("</td>")
                .append(String.format(Locale.US,"<td>%.10f</td><td>%.10f</td><td>%.10f</td><td>%.10f</td></tr>",r.homoHartree(),r.lumoHartree(),r.gapHartree(),r.gapEv()));
        }
        html.append("</tbody></table><h2>Observações</h2><pre>").append(escape(observations)).append("</pre>")
            .append("<p>Último par Alpha completo. Conversão: 1 Hartree = 27.211386245988 eV. A presença de orbitais não comprova convergência ou término normal do cálculo.</p>")
            .append("<p>Java prepara e analisa dados. Gaussian/PySCF executam os cálculos quânticos externamente.</p></html>");
        try { Files.writeString(destination,html); }
        catch(IOException e){throw new ReportWriteException("Falha ao gravar o relatório HTML.",e);}
    }
    private static String escape(String text) {
        return Objects.requireNonNullElse(text,"").replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;");
    }
}
