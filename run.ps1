param([switch]$Test, [switch]$Web)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $mavenCommand = Get-Command mvn -ErrorAction SilentlyContinue
    if ($mavenCommand) {
        $mavenPath = $mavenCommand.Source
    } else {
        $jetbrainsPath = Join-Path $env:ProgramFiles 'JetBrains'
        $bundledMaven = Get-ChildItem -Path $jetbrainsPath -Filter mvn.cmd -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $bundledMaven) { throw 'Maven não encontrado. Instale Maven no PATH ou use o painel Maven do IntelliJ.' }
        $mavenPath = $bundledMaven.FullName
    }
    if ($Test) { & $mavenPath test }
    elseif ($Web) { & $mavenPath compile exec:java '-Dexec.args=--web' }
    else { & $mavenPath compile exec:java }
    if ($LASTEXITCODE -ne 0) { throw 'A execução Maven falhou. Consulte a saída acima.' }
} finally { Pop-Location }
