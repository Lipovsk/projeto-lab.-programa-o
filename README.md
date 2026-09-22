# Analisador HOMO-LUMO

Interface local em HTML, CSS e JavaScript, com análise em Java 21 e sem bibliotecas externas.

## Executar

No IntelliJ IDEA, execute `org.example.Main`. A pasta `src/main/resources` deve estar marcada como Resources Root (padrão Maven).

Ou, no PowerShell na pasta do projeto, com JDK 21 no PATH:

```powershell
.\run.ps1
```

O programa abre o navegador padrão. Se a abertura automática não estiver disponível, acesse o endereço exibido no terminal. Selecione um arquivo `.out`, `.log` ou `.txt` e clique em **ANALISAR**. Encerre a execução Java (ou use Ctrl+C no terminal) ao terminar.

## Organização

- `Main`: inicia a aplicação.
- `LocalServer`: serve a página em 127.0.0.1, recebe o arquivo e retorna os resultados.
- `HomoLumoAnalyzer`: lógica original de leitura e cálculo.
- `Resultado`: record com as quatro energias.
- `src/main/resources/web`: interface, estilos e interações.

O navegador mantém uma referência ao arquivo selecionado e mostra seu nome; por segurança, não fornece o caminho absoluto original. O Java recebe o conteúdo em fluxo, grava uma cópia temporária, valida sua existência, analisa com BufferedReader e remove a cópia ao final. O arquivo Gaussian não é carregado inteiro na memória. Nenhum dado é enviado a serviços externos.

A análise preserva o comportamento original: usa as linhas Alpha, guarda o último par completo encontrado e calcula `(LUMO - HOMO) * 27.211386245988`. Não foram acrescentadas interpretações de linhas Beta ou de outros formatos científicos. Os erros aparecem em um diálogo na página.

## Testar sem dependências

```powershell
.\run.ps1 -Test
```

Os testes verificam energias, conversão, orbitais distribuídos em várias linhas, último par, valores ausentes, par incompleto, número inválido e arquivo inexistente. Não requerem JUnit ou Maven. São executados explicitamente pelo script (não pelo Surefire).
