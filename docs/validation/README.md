# Reprodução e evidências da entrega

Estes arquivos são auxiliares de documentação e validação. Não fazem parte do código de produção nem alteram o pom.xml.

## Entradas e saídas

- `prepare_inputs.py`: constrói uma geometria idealizada C2H6O com nove átomos, sem otimização; grava etanol.xyz e sua cópia 1.xyz. Produz CSV com etanol/benzeno e copia as linhas do OUT de teste, acrescentando identificação sintética em ISO-8859-1.
- `GenerateDelivery.java`: chama FileCoordinateProvider, SmilesService, GaussianInputWriter, GaussianBatchWriter, GaussianOutputParser, ResultCsvWriter, HtmlReportWriter, CsvMoleculeReader e BatchGenerationService. Grava `geracao.txt`.
- `compare_parsers.py`: executa uma referência Python independente sobre a mesma amostra conferida por SHA-256; lê os valores Java do CSV produzido pelo projeto. Grava o CSV comparativo e `comparacao.json`.
- `render_report.py`: converte o Markdown técnico em PDF e renderiza páginas para conferência. Requer ReportLab, PyMuPDF e fonte Arial do Windows, sem dependências Java adicionais.
- `audit_delivery.py`: verifica arquivos, valores, relatórios Surefire, JAR e ausência de caminhos pessoais nas saídas; registra `auditoria.json`.
- `maven-build.txt`: log real de `mvn -B clean test package`, com os caminhos pessoais substituídos por `<PROJECT>`/`<USER>`.

A amostra OUT é sintética e não contém energias calculadas para o etanol. O CSV inclui benzeno sem fornecer 2.xyz: a falha desse registro é esperada. A geração do lote foi exercitada em diretório temporário sob target, removido pelo clean final.

## Reprodução no PowerShell

Requer Java 21, Maven no PATH e Python 3. Scripts substituem somente os exemplos e as saídas de entrega de mesmo nome; execute em uma cópia se desejar conservar versões anteriores.

```powershell
python docs/validation/prepare_inputs.py
mvn -B compile dependency:build-classpath "-Dmdep.outputFile=target/delivery-classpath.txt"
$deliveryClasspath = "target/classes;" + (Get-Content target/delivery-classpath.txt -Raw).Trim()
java "-Duser.timezone=America/Fortaleza" --class-path $deliveryClasspath docs/validation/GenerateDelivery.java
python docs/validation/compare_parsers.py
```

A função Python usa leitura integral e expressão regular para selecionar o último par completo. É uma referência equivalente à descrição do roteiro, não uma transcrição certificada da função publicada pelos autores. A comparação executada comprova igualdade nesta amostra; não é benchmark nem validação geral de todos os formatos.

## Tratamento do BCF

GaussianBatchWriter grava caminhos absolutos. O runner chama essa classe e depois retira apenas o prefixo da pasta resultados, deixando `exemplo.gjf, exemplo.out`. Esse pós-processamento é explícito para não entregar caminhos pessoais fixos; GJF, CSV de resultados e HTML não recebem edição posterior.

**BCF gerado estruturalmente pelo ChemEST Java; execução no Gaussian não validada.**

Use o BCF com a pasta resultados como base ou, preferencialmente, gere novamente pelo aplicativo na instalação Gaussian de destino. O arquivo exemplo.out citado no BCF é o destino futuro do cálculo; ele não existe nesta entrega. Não confundir com a entrada sintética data/exemplos/gaussian_sample.out.

## PDF e build

As ferramentas documentais utilizadas nesta entrega foram Python 3.14.6, ReportLab 5.0.1, PyMuPDF 1.28.2 e pypdf 6.19.0, instaladas em diretório temporário. Para reproduzir, instale-as em ambiente virtual próprio e execute:

```powershell
python docs/validation/render_report.py
mvn -B clean test package
python docs/validation/audit_delivery.py
```

O clean remove imagens intermediárias sob target. A geração final do PDF foi repetida após o build apenas para registrar o resultado confirmado e conferir o layout; não houve alteração de código.

A entrega original registrou 29 testes aprovados. A revisão de 01/10/2026 executou 51 testes, zero falhas, zero erros e zero pulos. A auditoria aceita novos testes e registra alterações locais sem exigir uma árvore Git limpa; ela inspeciona evidências existentes e não executa Maven nem certifica seu código de saída. O JAR comum está em target/chemest-java-1.0-SNAPSHOT.jar; não incorpora dependências e não foi copiado para as pastas de entrega. target e .idea permanecem ignorados. Não foram realizados commit ou push.

