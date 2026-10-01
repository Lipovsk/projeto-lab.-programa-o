# ChemEST Java

Aplicativo Java 21 com interface principal **Swing**, baseado no trabalho “Integrating Electronic Structure Theory Calculations with Python and Cheminformatics Tools”.

O Java prepara entradas, organiza lotes e analisa energias de orbitais. **Não implementa nem executa DFT/Hartree–Fock. Gaussian/PySCF são responsáveis pelo cálculo quântico externo.** A leitura de saída implementada aqui é específica dos blocos Alpha do Gaussian; saídas PySCF não são interpretadas automaticamente.

## Requisitos e instalação

- JDK **21** e Maven 3.9 ou compatível.
- Ambiente gráfico para Swing.
- Internet na primeira compilação para resolver dependências.
- Abra a pasta do projeto ou seu `pom.xml` como projeto Maven.
- Não é necessário instalar Gaussian para compilar, testar ou analisar a amostra sintética.

Dependências solicitadas e utilizadas, sem substituições:

| Biblioteca | Coordenada Maven | Versão |
|---|---|---|
| CDK | org.openscience.cdk:cdk-bundle | 2.13 |
| Apache Commons CSV | org.apache.commons:commons-csv | 1.14.1 |
| JUnit 5 | org.junit.jupiter:junit-jupiter | 5.13.4 |

Identificação: `br.edu.unit:chemest-java:1.0-SNAPSHOT`.

## Executar

Na pasta do projeto:

```sh
mvn compile exec:java
```

No Windows também é possível usar `.\run.ps1`. O script procura Maven no PATH e, se necessário, o Maven integrado ao IntelliJ sob Program Files/JetBrains. Não contém caminhos pessoais fixos.

### IntelliJ IDEA — passo a passo

1. **File → Open**: selecione `pom.xml` e abra como projeto.
2. **File → Project Structure → Project SDK**: selecione um JDK 21; defina o language level como 21.
3. No painel **Maven**, clique em **Reload All Maven Projects** e aguarde as dependências.
4. Abra `src/main/java/br/edu/unit/chemest/App.java`.
5. Clique no triângulo verde ao lado de `main` e selecione **Run 'App.main()'**, sem argumentos.
6. Para testes, execute **Maven → Lifecycle → test**; para empacotar, **package**.

O Maven Runner também deve usar JDK 21. A classe principal mudou de `org.example.Main` para `br.edu.unit.chemest.App`.

## Fluxo de uso

### MOLÉCULA e ENTRADA GAUSSIAN

1. Digite um nome e `CCO` em SMILES, depois clique **Validar**: o CDK representa o etanol em 2D.
2. `c1ccccc1` é um exemplo aromático. `C1(` deve mostrar um aviso sem encerrar o aplicativo.
3. Importe um `.xyz` com a geometria desejada. Alterar o SMILES descarta a associação anterior de coordenadas.
4. Na aba ENTRADA GAUSSIAN, configure método, base, tarefa, carga, multiplicidade, memória e processadores.
5. Clique **Gerar GJF** e escolha o destino.

Os valores iniciais são B3LYP/6-31G(d), Opt, carga 0, multiplicidade 1, 2GB e 2 processadores. São valores editáveis de exemplo, não uma recomendação para todos os sistemas.

**Desenho 2D não é geometria 3D.** O CDK desenha ligações para visualização; essas posições não são usadas para gerar GJF. Sem XYZ importado, a geração é bloqueada. O importador exige:

- Número positivo de átomos na primeira linha.
- Uma segunda linha de comentário, que pode estar vazia.
- Uma linha `símbolo x y z` por átomo, com três números finitos em **angstroms**.
- Um único conjunto de coordenadas por arquivo.
- Símbolos químicos reais; não aceita átomos fictícios.

Uma geometria explicitamente planar, inclusive com todos os z iguais a zero, é válida. O aplicativo nunca preenche uma dimensão ausente. Inclua todos os hidrogênios necessários. A correspondência química entre XYZ e SMILES, conectividade, estereoquímica, unidades, qualidade da geometria e consistência carga/multiplicidade devem ser conferidas pelo usuário; o importador valida a estrutura dos dados, não realiza otimização nem validação física.

### LOTE

CSV UTF-8 separado por vírgulas, com colunas acessadas **pelo nome**; a ordem pode variar:

```csv
NUM,SMILES
1,CCO
2,c1ccccc1
```

Selecione o CSV e uma pasta contendo `1.xyz`, `2.xyz`, etc. **Gerar GJF do lote** cria `1.gjf`, `2.gjf` nessa mesma pasta, usando a configuração atual. Registros inválidos ou sem geometria falham individualmente e aparecem na tabela e no log. O resumo mostra processados, gerados e falhas. Arquivos GJF existentes não são sobrescritos pelo lote.

NUM aceita letras, números, hífen e sublinhado; valores repetidos, inclusive variações de maiúsculas, são rejeitados para evitar colisões. Erros estruturais que impossibilitam interpretar o CSV, como aspas não fechadas, interrompem a importação com mensagem.

**Gerar BCF** inclui apenas arquivos regulares `.gjf`, ordenados pelo nome, com cabeçalho:

```text
!
!batch file
!start=1
!
```

Cada entrada contém caminho GJF e caminho OUT separados por vírgula. Os caminhos são montados com `Path.resolve`. Caminhos com vírgula ou quebra de linha são rejeitados. O BCF usa caminhos absolutos do computador em que foi gerado; regenere-o após mover a pasta. A execução no Gaussian licenciado não foi validada neste ambiente.

### ANÁLISE

1. Clique **Selecionar OUT** e escolha um ou mais arquivos `.out`, `.log` ou `.txt`.
2. Clique **Analisar arquivos**. Os resultados são acrescentados à tabela; arquivos inválidos são detalhados no LOG.
3. Use **Exportar CSV** ou **Exportar relatório HTML**.

Exemplo disponível: `src/test/resources/gaussian_sample.out`, uma amostra **sintética** pequena.

| HOMO | LUMO | Gap Hartree | Gap eV |
|---|---|---|---|
| -0.250000 | -0.050000 | 0.200000 | 5.4422772491976 |

O parser preserva a leitura linha por linha com `BufferedReader`, em ISO-8859-1, e guarda o último par Alpha completo. Usa o último ocupado e o primeiro virtual correspondente, inclusive quando há várias linhas. Um bloco ocupado incompleto ao final não substitui um par anterior completo. Sem qualquer par completo, lança `GaussianParseException`. Valores inválidos também são rejeitados; problemas reais de arquivo usam `IOException`.

```text
gapHartree = LUMO - HOMO
gapEv = gapHartree * 27.211386245988
```

O CSV contém `arquivo,HOMO,LUMO,gapHartree,gapEv`, com ponto decimal e escape de campos via Commons CSV. O HTML é autocontido, abre diretamente no navegador e contém data, origem, energias, observações e configuração atual da interface. Textos são escapados. A configuração exibida é identificada como referência da interface, **não como informação extraída do OUT**.

### LOG e responsividade

O log mostra horário, operação, arquivo e resultado/erro. Leitura, desenho, geração e exportação usam `SwingWorker`; os botões ficam desabilitados durante a operação e o status mostra atividade. Erros não apresentam stack traces ao usuário.

## Testar e empacotar

```sh
mvn test
mvn package
```

Alternativa Windows: `.\run.ps1 -Test`.

Os testes JUnit cobrem parser original, último par completo, ausência/incompletude, valores conhecidos e conversão com tolerância `1e-8`; SMILES válido/aromático/inválido; validação de modelos; XYZ, bloqueio sem geometria e formato GJF; CSV/lote/BCF; relatório HTML e exportação CSV; continuidade de análise após falha.

O smoke test Swing inicia `App`, valida CCO, exercita o diálogo de SMILES inválido e confirma recuperação. Em ambientes sem display, apenas esse teste é pulado explicitamente. Ele grava uma imagem de verificação em `target/swing-smoke.png`, ignorada pelo Git.

`mvn package` gera `target/chemest-java-1.0-SNAPSHOT.jar`. É um JAR comum, sem dependências embutidas; execute pelo IntelliJ ou por `mvn compile exec:java`.

## Arquitetura

```text
br.edu.unit.chemest
├── App
├── model         Atom3D, MoleculeRecord, CalculationConfig, OrbitalResult, AnalysisRecord
├── service       SmilesService, CoordinateProvider, FileCoordinateProvider,
│                 BatchGenerationService, AnalysisService e exceções
├── io            CsvMoleculeReader, GaussianInputWriter, GaussianBatchWriter,
│                 GaussianOutputParser, ResultCsvWriter, GaussianParseException
├── report        HtmlReportWriter, ReportWriteException
├── ui            MainFrame
└── experimental  LocalServer
```

Modelos guardam dados e invariantes; serviços conduzem o fluxo; IO trata arquivos; relatório formata a saída; Swing coleta entradas e apresenta resultados. A lógica científica não fica no JFrame.

## Interface web preservada

A interface HTML/CSS/JavaScript original continua como extra experimental:

```sh
mvn compile exec:java -Dexec.args=--web
```

Ou `.\run.ps1 -Web`. O servidor local usa 127.0.0.1 e porta disponível. Para encerrar, pare a execução Java. O upload continua em fluxo para um arquivo temporário, removido após a análise, e agora usa o mesmo GaussianOutputParser. Swing é a interface principal; o relatório HTML é uma saída independente.

## Limitações e itens opcionais

- Apenas orbitais Alpha; sem interpretação Beta, TD-DFT ou saídas PySCF.
- Não verifica convergência, término normal ou completude física do cálculo.
- Não executa programas quânticos nem inclui arquivos licenciados Gaussian.
- Geometria: XYZ explícito. Importação SDF e busca PubChem ficam como extensões opcionais não implementadas.
- Não gera ou otimiza coordenadas a partir do SMILES.
- Não confere se o XYZ corresponde ao SMILES nem se o método/base está instalado no Gaussian.
- Resultados e log ficam em memória até fechar; exporte o que desejar conservar.
- BCF gerado e testado estruturalmente, sem execução no Gaussian.
- Os testes de exportação validam arquivos gerados; não automatizam o navegador nem todos os diálogos de seleção de arquivos.

## Higiene do repositório

`target/`, arquivos temporários, configurações IDE e arquivos locais de ambiente estão ignorados. Não inclua senhas, tokens, chaves ou dados licenciados.

A pasta `.idea` **já estava versionada** antes desta migração. O .gitignore não remove arquivos previamente rastreados. Recomenda-se executar `git rm --cached -r .idea` em uma alteração própria para retirar do Git **sem apagar as configurações locais**. Essa remoção do índice não foi executada; a alteração local de `.idea/workspace.xml` foi preservada.

O inventário e a análise da migração estão em [docs/MIGRATION.md](docs/MIGRATION.md).

## Referências de implementação

- [CDK DepictionGenerator](https://cdk.github.io/cdk/latest/docs/api/org/openscience/cdk/depict/DepictionGenerator.html)
- [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html)
- [Tutorial Gaussian do Barrett Research Group, seção BCF](https://barrett-group.mcgill.ca/tutorials/Gaussian%20tutorial.pdf)
