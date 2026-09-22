param([switch]$Test)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    New-Item -ItemType Directory -Force target/app-classes | Out-Null
    $sources = @(Get-ChildItem src/main/java/org/example/*.java | ForEach-Object FullName)
    & javac -encoding UTF-8 -d target/app-classes $sources
    if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação Java.' }
    Copy-Item -LiteralPath src/main/resources/web -Destination target/app-classes -Recurse -Force
    if ($Test) {
        & javac -encoding UTF-8 -d target/test-classes $sources src/test/java/org/example/HomoLumoAnalyzerTest.java
        if ($LASTEXITCODE -ne 0) { throw 'Falha na compilação dos testes.' }
        & java -cp target/test-classes org.example.HomoLumoAnalyzerTest
        if ($LASTEXITCODE -ne 0) { throw 'Falha nos testes.' }
    } else {
        & java -cp target/app-classes org.example.Main
        if ($LASTEXITCODE -ne 0) { throw 'Falha ao executar a aplicação.' }
    }
} finally {
    Pop-Location
}
