# Análise e inventário da migração

## Antes das alterações

Arquivos funcionais encontrados:

- `pom.xml`, `README.md`, `run.ps1`, `.gitignore`.
- `src/main/java/org/example/Main.java`.
- `src/main/java/org/example/LocalServer.java`.
- `src/main/java/org/example/HomoLumoAnalyzer.java`.
- `src/main/java/org/example/Resultado.java`.
- `src/test/java/org/example/HomoLumoAnalyzerTest.java`.
- `src/main/resources/web/index.html`, `styles.css`, `app.js`.
- Diretórios auxiliares: `.git`, `.idea`, `.mvn` (sem wrapper), `.github/modernize/java-upgrade` (hooks auxiliares) e `target`.

Main iniciava LocalServer; o navegador enviava o arquivo em fluxo; o servidor criava uma cópia temporária e chamava HomoLumoAnalyzer. O analisador lia ISO-8859-1 por BufferedReader, atualizava o HOMO temporário a cada linha ocupada e salvava o par ao encontrar a primeira linha virtual. A última linha virtual de continuação não substituía o LUMO.

HOMO era o último número ocupado; LUMO, o primeiro virtual. Gap era LUMO-HOMO e a conversão usava 27.211386245988. Arquivo ausente e orbitais ausentes lançavam IllegalArgumentException; erros de leitura propagavam IOException. Os testes eram um main com asserções manuais, sem integração Surefire.

O teste legado foi executado e passou antes da adaptação. A compilação Maven inicial passou, mas executava zero testes JUnit, como esperado antes da conversão.

## Reutilização

O algoritmo de leitura e pareamento do parser foi adaptado para GaussianOutputParser, preservando leitura em fluxo e último par completo. Resultado virou OrbitalResult; os gaps são derivados de HOMO/LUMO. Foram acrescentados erros específicos, validação finita e suporte à notação numérica D.

A interface web e seus recursos foram preservados como opção experimental. LocalServer foi migrado com a alteração local de formatação já existente, trocando apenas pacote e integração com o novo parser/modelo. A configuração local da IDE não foi alterada.

## Arquivos criados

Sob `src/main/java/br/edu/unit/chemest/`:

- `App.java`.
- `model/Atom3D.java`, `MoleculeRecord.java`, `CalculationConfig.java`, `OrbitalResult.java`, `AnalysisRecord.java`.
- `service/SmilesService.java`, `InvalidSmilesException.java`, `CoordinateProvider.java`, `FileCoordinateProvider.java`, `CoordinateException.java`, `BatchGenerationService.java`, `AnalysisService.java`.
- `io/CsvMoleculeReader.java`, `GaussianInputWriter.java`, `GaussianBatchWriter.java`, `GaussianOutputParser.java`, `GaussianParseException.java`, `ResultCsvWriter.java`.
- `report/HtmlReportWriter.java`, `ReportWriteException.java`.
- `ui/MainFrame.java`.
- `experimental/LocalServer.java` (migrado).

Sob `src/test/java/br/edu/unit/chemest/`:

- `GaussianOutputParserTest.java`, `SmilesServiceTest.java`, `CoordinatesAndInputTest.java`, `BatchTest.java`, `ReportTest.java`, `ModelAndAnalysisTest.java`, `SwingSmokeTest.java`.

Também: `src/test/resources/gaussian_sample.out` e este documento.

## Arquivos modificados

`pom.xml`, `README.md`, `run.ps1`, `.gitignore`.

## Arquivos removidos após migração

Os quatro arquivos Java de `org/example` e o teste manual `HomoLumoAnalyzerTest.java`. As funcionalidades correspondentes foram migradas; os três recursos web foram mantidos sem alterações.

## Validação por fase

1. Maven/modelos: compilação passou e teste legado manual passou.
2. Parser: 10 testes JUnit passaram.
3. CDK: 14 testes passaram.
4. Coordenadas/GJF: 20 testes passaram.
5. CSV/lote/BCF: 23 testes passaram.
6. Swing: compilação e 23 testes passaram.
7. Exportação/HTML: 25 testes passaram.
8. Integração e smoke Swing: 29 testes passaram, sem pulos no ambiente gráfico local.
9. Documentação e remoção dos fontes antigos: mvn -B clean test package terminou com BUILD SUCCESS: 29 testes, zero falhas, zero erros e zero pulos. JAR gerado em target/chemest-java-1.0-SNAPSHOT.jar.

O mínimo obrigatório foi implementado. SDF e PubChem são extensões opcionais pendentes. Nenhum cálculo quântico foi executado; a compatibilidade do BCF com uma instalação licenciada de Gaussian não foi testada.
