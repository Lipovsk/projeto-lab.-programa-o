# ChemEST Java

O **ChemEST Java** é uma aplicação acadêmica em **Java 21**, baseada no artigo *“Integrating Electronic Structure Theory Calculations with Python and Cheminformatics Tools”*. O Java prepara entradas, organiza lotes e analisa resultados; **Gaussian/PySCF continuam responsáveis pelos cálculos quânticos externos**. O projeto não reimplementa DFT ou Hartree-Fock.

## Objetivo

Adaptar partes do fluxo original em Python para uma aplicação Java que permita validar SMILES, importar geometria 3D, gerar arquivos Gaussian, analisar suas saídas, extrair energias HOMO/LUMO, calcular o gap e exportar resultados. A integração implementada concentra-se em arquivos Gaussian; não há execução nem interpretação automática de saídas PySCF.

## Funcionalidades

- Interface principal em **Java Swing**, com cinco abas: **Molécula**, **Entrada Gaussian**, **Lote**, **Análise** e **Log**.
- Validação de SMILES com CDK e representação molecular 2D.
- Importação de coordenadas XYZ e geração de arquivos `.gjf`.
- Leitura de CSV com colunas `NUM` e `SMILES`, validação por registro e geração de GJF em lote.
- Geração de BCF com os arquivos GJF da pasta, ordenados.
- Análise de arquivos `.out`, `.log` e `.txt` que contenham os padrões Gaussian reconhecidos.
- Extração de HOMO/LUMO do último par Alpha completo, cálculo do gap em Hartree e conversão para eV.
- Exportação de resultados em CSV e relatório HTML independente de servidor.
- Tratamento de erros com mensagens compreensíveis e logs das operações.
- Uso de `SwingWorker` para executar leitura, desenho, geração e exportação em segundo plano, mantendo a interface responsiva.

## Tecnologias

| Tecnologia | Versão/configuração no projeto | Uso |
|---|---|---|
| Java | 21 | Linguagem e ambiente de execução |
| Maven | Projeto `br.edu.unit:chemest-java:1.0-SNAPSHOT` | Compilação, dependências e testes |
| Java Swing | Incluído no JDK | Interface principal |
| CDK | `org.openscience.cdk:cdk-bundle:2.13` | SMILES e desenho 2D |
| Apache Commons CSV | `org.apache.commons:commons-csv:1.14.1` | Leitura e exportação CSV |
| JUnit 5 | `org.junit.jupiter:junit-jupiter:5.13.4` | Testes automatizados |

As três dependências usam exatamente as versões previstas no roteiro. O `pom.xml` configura Maven Compiler Plugin **3.14.0**, Surefire **3.5.4** e Exec Maven Plugin **3.5.1**; não fixa a versão da distribuição Maven.

Para executar, tenha JDK 21, Maven disponível e ambiente gráfico. A primeira resolução de dependências requer internet. Gaussian não é necessário para compilar, testar ou analisar a amostra sintética.

Documentação técnica: [CDK DepictionGenerator](https://cdk.github.io/cdk/latest/docs/api/org/openscience/cdk/depict/DepictionGenerator.html) e [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/apidocs/org/apache/commons/csv/CSVFormat.html).

## Estrutura do projeto

Fontes principais em `src/main/java/`:

```text
br.edu.unit.chemest
├── App
├── model
├── service
├── io
├── report
└── ui
```

| Componente | Responsabilidade |
|---|---|
| `App` | Inicia a interface Swing |
| `model` | Dados e invariantes: átomos, moléculas, configuração e resultados |
| `service` | Validação SMILES, leitura de geometria e coordenação de análise/lotes |
| `io` | Leitura CSV/OUT e escrita GJF, BCF e CSV de resultados |
| `report` | Geração do relatório HTML |
| `ui` | `MainFrame`, componentes Swing e eventos |

Os testes ficam em `src/test/java/br/edu/unit/chemest/`; a amostra Gaussian está em `src/test/resources/gaussian_sample.out`. O histórico da migração está em [docs/MIGRATION.md](docs/MIGRATION.md).

## Como executar no IntelliJ IDEA

1. Use **File → Open**, selecione `pom.xml` e abra como projeto Maven.
2. Em **File → Project Structure**, selecione **JDK 21** e language level 21. Configure também o Maven Runner para usar esse JDK.
3. No painel **Maven**, clique em **Reload All Maven Projects** e aguarde as dependências.
4. Abra `br.edu.unit.chemest.App`, em `src/main/java/br/edu/unit/chemest/App.java`.
5. Execute o método `main` pelo triângulo verde, sem argumentos.

No terminal, na pasta do projeto:

```sh
mvn compile exec:java
```

No PowerShell, use também `.\run.ps1`. O script procura Maven no PATH e, como alternativa, na instalação do IntelliJ sob `Program Files/JetBrains`, sem caminho pessoal fixo.

## Como usar a aplicação

### Molécula

1. Informe o nome e um SMILES, como `CCO` (etanol).
2. Clique em **Validar** para visualizar a estrutura 2D. `c1ccccc1` é um exemplo aromático; `C1(` é inválido e provoca uma mensagem de erro.
3. Clique em **Importar coordenadas XYZ** e selecione a geometria correspondente.

**Representação 2D não é geometria 3D.** O desenho do CDK não fornece as coordenadas utilizadas no GJF. Alterar o SMILES descarta a associação anterior de geometria, exigindo nova importação.

O XYZ deve conter quantidade positiva de átomos, uma linha de comentário (pode estar vazia) e uma linha `símbolo x y z` por átomo. Forneça um único conjunto de coordenadas, em **angstroms**, com números finitos e símbolos químicos válidos. Inclua todos os átomos necessários, inclusive hidrogênios.

Geometrias explicitamente planares são aceitas, mesmo com todos os valores z iguais a zero. O programa não preenche coordenadas ausentes nem verifica se o XYZ corresponde quimicamente ao SMILES.

### Entrada Gaussian

Ajuste os parâmetros antes de clicar em **Gerar GJF**:

| Campo | Valor inicial |
|---|---|
| Método | `B3LYP` |
| Base | `6-31G(d)` |
| Tarefa | `Opt` |
| Carga | `0` |
| Multiplicidade | `1` |
| Memória | `2GB` |
| Processadores | `2` |

São valores editáveis de exemplo. Processadores e multiplicidade devem ser pelo menos 1; memória, método, base e tarefa são obrigatórios. A geração é bloqueada quando não há geometria importada.

O arquivo inclui recursos, rota de cálculo, nome/SMILES, carga, multiplicidade e coordenadas com ponto decimal (`Locale.US`). A interface pede confirmação antes de substituir um destino existente.

### Lote

Prepare um CSV UTF-8 separado por vírgulas:

```csv
NUM,SMILES
1,CCO
2,c1ccccc1
```

1. Clique em **Selecionar CSV**. As colunas são lidas pelo nome, independentemente da ordem.
2. Clique em **Selecionar pasta** e escolha a pasta com `1.xyz`, `2.xyz`, etc.
3. Use **Gerar GJF do lote** para produzir `1.gjf`, `2.gjf` na mesma pasta, com a configuração atual.
4. Use **Gerar BCF** para listar os GJF da pasta em um arquivo de controle de lote.

A tabela informa o status individual; o resumo mostra processados, gerados e falhas. Registros inválidos ou sem geometria não impedem os demais. O lote não sobrescreve GJFs existentes.

`NUM` aceita letras ASCII, números, hífen e sublinhado; duplicatas são rejeitadas sem distinção entre maiúsculas e minúsculas. Nomes reservados pelo Windows, como `CON` e `LPT1`, são rejeitados por registro. CSV UTF-8 com BOM é aceito; cabeçalhos repetidos são rejeitados. Um CSV estruturalmente ilegível, por exemplo com aspas não fechadas, interrompe a importação.

O BCF inclui apenas arquivos regulares `.gjf`, ordenados pelo nome, e começa com:

```text
!
!batch file
!start=1
!
```

Cada entrada contém caminhos GJF e OUT separados por vírgula, construídos com `Path.resolve`. Caminhos com vírgulas ou quebras de linha são rejeitados. Como os caminhos gravados são absolutos, regenere o BCF após mover a pasta. Referência do formato: [tutorial Gaussian do Barrett Research Group](https://barrett-group.mcgill.ca/tutorials/Gaussian%20tutorial.pdf).

### Análise

Clique em **Selecionar OUT**, escolha um ou mais arquivos `.out`, `.log` ou `.txt` e pressione **Analisar arquivos**. A tabela apresenta:

| Coluna | Conteúdo |
|---|---|
| Arquivo | Origem do resultado |
| HOMO (Hartree) | Energia do último orbital ocupado |
| LUMO (Hartree) | Energia do primeiro orbital virtual correspondente |
| Gap (Hartree) | Diferença LUMO − HOMO |
| Gap (eV) | Diferença convertida para elétron-volts |

Os resultados são acrescentados à tabela. Falhas de um arquivo são registradas no Log e não interrompem os demais.

Cada clique em **ANALISAR** substitui a tabela pelos resultados dos arquivos selecionados, evitando duplicatas de análises anteriores. Se todos falharem, a tabela e os indicadores ficam vazios. Falhas de análise ou lote aparecem no resumo e no status, sem indicação verde de sucesso total.

**Exportar CSV** grava `arquivo,HOMO,LUMO,gapHartree,gapEv`, com ponto decimal e escape de campos pelo Commons CSV. A exportação CSV não depende dos campos da configuração Gaussian. **Exportar relatório HTML** permite incluir observações e gera um documento que abre diretamente no navegador, sem servidor.

O HTML contém data, origem, resultados e configuração atual da interface, com textos escapados. Essa configuração é uma referência para gerar entradas: **não é extraída dos arquivos OUT** nem comprova os parâmetros dos cálculos analisados. Na implementação atual, ambas as exportações passam pela validação dos campos de configuração.

### Log

Registra horário, operação, arquivo ou identificador e resultado, incluindo mensagens de erro. Durante operações em segundo plano, os botões ficam desabilitados e o status indica atividade; são habilitados novamente ao concluir. O usuário recebe mensagens de erro, sem stack trace bruto.

## Como funciona o parser HOMO/LUMO

`GaussianOutputParser` usa `BufferedReader`, com codificação ISO-8859-1, para ler o arquivo **linha por linha**, sem carregar todo o conteúdo na memória. Ele procura:

```text
Alpha  occ. eigenvalues --
Alpha virt. eigenvalues --
```

A cada linha ocupada, atualiza o HOMO temporário com o **último valor**. Na primeira linha virtual correspondente, usa o **primeiro valor** como LUMO e guarda o par completo. Linhas virtuais de continuação não substituem esse LUMO.

O parser aceita variações de espaços e notação `D`/`d`, valida todas as energias nas linhas utilizadas e exige linhas ocupadas/virtuais consecutivas; uma linha de outro bloco interrompe o pareamento pendente. O resultado é o **último par Alpha completo** encontrado. Um bloco ocupado incompleto no final não substitui um par anterior completo. Se nenhum par completo existir, lança `GaussianParseException`; valores inválidos também são rejeitados. Problemas reais de arquivo usam `IOException`.

`OrbitalResult` calcula:

```text
gapHartree = LUMO - HOMO
gapEv = gapHartree * 27.211386245988
```

A versão atual trata apenas orbitais **Alpha**, sem interpretação Beta.

## Melhorias em relação ao código Python original

O repositório não inclui o código Python original para uma comparação direta. Os pontos abaixo descrevem recursos confirmados na adaptação Java, sem afirmar que todos estavam ausentes no original:

- Leitura do OUT linha por linha, evitando manter uma cópia integral do arquivo em memória e reduzindo essa necessidade de armazenamento em relação à leitura integral.
- Tratamento explícito de arquivos, SMILES, coordenadas e configurações inválidos.
- Cálculo automático do gap e conversão para eV.
- Organização orientada a objetos, com dados, serviços, arquivos e interface separados.
- Interface gráfica Swing, exportação de resultados e testes automatizados.

Não há benchmark no projeto; portanto, não se afirma que a implementação Java seja mais rápida.

## Exemplo de resultado

A amostra **sintética** [gaussian_sample.out](src/test/resources/gaussian_sample.out), utilizada pelos testes, produz:

```text
HOMO = -0.250000 Hartree
LUMO = -0.050000 Hartree
Gap  =  0.200000 Hartree
Gap  ≈  5.442277 eV
```

## Testes

Execute:

```sh
mvn test
```

No Windows, também é possível usar `.\run.ps1 -Test`.

| Classe de teste | Cobertura |
|---|---|
| `GaussianOutputParserTest` | Valores conhecidos, último par completo, múltiplas linhas, bloco incompleto, dados ausentes/inválidos, arquivo inexistente e conversão para eV |
| `SmilesServiceTest` | CCO, molécula aromática, desenho 2D e SMILES inválido/vazio |
| `CoordinatesAndInputTest` | XYZ, coordenadas ausentes/não finitas, geometria planar, bloqueio sem geometria e formato GJF com ponto decimal |
| `BatchTest` | CSV por nome de coluna, validação individual, duplicatas, geração em lote, proteção contra sobrescrita e ordenação/filtro BCF |
| `ModelAndAnalysisTest` | Configuração, imutabilidade dos átomos e continuidade da análise após erro |
| `IoRegressionTest` | BOM CSV/XYZ, cabeçalhos repetidos, nomes Windows, linhas inválidas, falhas de gravação, publicação sem sobrescrita e destinos BCF |
| `ParserRegressionTest` | Espaçamento, expoentes D/d, separação de blocos e valores inválidos no interior de linhas |
| `SwingRegressionTest` | Reanálise sem duplicatas, status de falha, janela fechada e exportação CSV com configuração incompleta |
| `ReportTest` | Exportação CSV, valores numéricos, escape HTML e falha de gravação |
| `SwingSmokeTest` | Inicialização de App, cinco abas, CCO, diálogo de SMILES inválido e recuperação da interface |

As comparações numéricas usam tolerância `1e-8`. A revisão de 01/10/2026 executou `mvn -B clean test package`: **51 testes, sem falhas, erros ou pulos**, incluindo os testes gráficos Swing. O estado inicial tinha 29 testes aprovados; foram acrescentados 22 testes de regressão.

O smoke test requer ambiente gráfico e é explicitamente pulado em modo headless. Ele salva `target/swing-smoke.png`. Os testes não automatizam todos os diálogos de seleção nem executam Gaussian ou verificam o relatório em navegador.

## Empacotamento

```sh
mvn package
```

O artefato gerado é `target/chemest-java-1.0-SNAPSHOT.jar`. O `pom.xml` produz um **JAR comum, sem dependências embutidas**, e não configura um JAR executável autossuficiente. Use o IntelliJ ou `mvn compile exec:java` para iniciar a aplicação.

## Limitações

- Apenas blocos Alpha de saídas Gaussian; sem suporte Beta ou interpretação de saídas PySCF.
- Sem execução direta de Gaussian ou PySCF e sem implementação de DFT/Hartree-Fock.
- XYZ precisa ser fornecido; não há geração ou otimização de geometria a partir do SMILES.
- Sem validação física da geometria, conectividade, estereoquímica, correspondência XYZ/SMILES, unidades ou consistência carga/multiplicidade.
- Não verifica disponibilidade de método/base no Gaussian, convergência ou término normal do cálculo.
- Importação SDF e busca PubChem, previstas como possibilidades opcionais no roteiro, **não estão implementadas**.
- BCF é gerado e possui testes estruturais; sua execução em Gaussian licenciado **não foi validada**.
- Resultados e log permanecem em memória durante a sessão; exporte os resultados antes de fechar.

## Organização acadêmica

A separação entre `model`, `service`, `io`, `report` e `ui` segue o roteiro da faculdade e facilita a leitura, manutenção e verificação por testes. A interface coleta dados e apresenta resultados; o pareamento HOMO/LUMO fica no parser, e as fórmulas do gap ficam no modelo `OrbitalResult`.

## Segurança e higiene do repositório

- `target/`, arquivos temporários e configurações locais estão ignorados.
- **A pasta `.idea` foi removida do controle de versão e permanece ignorada pelo `.gitignore`.** Nenhum arquivo dela está atualmente rastreado; configurações locais podem continuar no computador.
- Não inclua senhas, tokens, chaves, arquivos privados de ambiente ou arquivos licenciados do Gaussian.
- Evite caminhos pessoais fixos no código e na documentação. O aplicativo recebe caminhos pelo seletor de arquivos; o BCF registra os caminhos do lote escolhido.

O documento de migração registra o estado histórico da IDE. O estado atual do Git é o descrito nesta seção.

## Referência do artigo

Han, D.; Han, H.; Chen, E.; Cesarski, W. J.; Lim, H.; Shin, H.; Cowart, S. V.; Nagelli, E. A.; Yuk, S. F.; Jeong, K.
*Integrating Electronic Structure Theory Calculations with Python and Cheminformatics Tools.*
**Journal of Chemical Education**, 2025, **102**, 4115–4122.
DOI: [10.1021/acs.jchemed.4c01171](https://doi.org/10.1021/acs.jchemed.4c01171).

## Observação final

Este projeto tem finalidade acadêmica. Java atua como camada de preparação, organização e análise de dados; Gaussian/PySCF são ferramentas externas responsáveis pelo cálculo científico.

### Gravação de arquivos

GJF, BCF, CSV de resultados e HTML são preparados em arquivo temporário na pasta de destino e publicados somente após a gravação completa. A substituição usa movimentação atômica quando suportada pelo sistema de arquivos; há alternativa sem garantia de atomicidade quando não suportada. O lote publica sem substituir GJFs existentes. BCF não pode usar como destino uma entrada GJF ou saída OUT incluída no lote. Ao fechar a janela, uma operação já iniciada pode terminar de gravar arquivos, mas sua conclusão não atualiza a janela nem abre diálogos.
