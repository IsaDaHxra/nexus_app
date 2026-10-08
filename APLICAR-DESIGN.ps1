param(
    [Parameter(Mandatory = $true)]
    [string]$ProjetoAtual
)
$ErrorActionPreference = 'Stop'
$origem = Join-Path $PSScriptRoot 'app/src/main/java/com/nexus/mobile'
$destinoRaiz = (Resolve-Path $ProjetoAtual).Path
$destino = Join-Path $destinoRaiz 'app/src/main/java/com/nexus/mobile'
if (-not (Test-Path (Join-Path $destino 'NexusViewModel.kt'))) {
    throw 'Informe a pasta raiz do Nexus que contém app, e não a pasta app.'
}
$arquivos = @(
    'MainActivity.kt',
    'ui/Design.kt',
    'ui/HomeScreen.kt',
    'ui/CharactersScreens.kt',
    'ui/ExtraScreens.kt',
    'ui/StoryScreen.kt',
    'ui/Motion.kt',
    'ui/PortalScreen.kt',
    'ui/JourneyScreen.kt',
    'ui/DimensionalCard.kt'
)
foreach ($arquivo in $arquivos) {
    if (-not (Test-Path (Join-Path $origem $arquivo))) { throw "Arquivo de origem ausente: $arquivo" }
}
if ($destinoRaiz -eq $PSScriptRoot) { throw 'Escolha seu projeto atual como destino, não a pasta deste pacote.' }
$backup = Join-Path $destinoRaiz ('Nexus-backup-design-' + (Get-Date -Format 'yyyyMMdd-HHmmss-fff'))
foreach ($arquivo in $arquivos) {
    $atual = Join-Path $destino $arquivo
    if (Test-Path $atual) {
        $copia = Join-Path $backup $arquivo
        New-Item -ItemType Directory -Path (Split-Path $copia) -Force | Out-Null
        Copy-Item -LiteralPath $atual -Destination $copia
    }
}
foreach ($arquivo in $arquivos) {
    $novo = Join-Path $destino $arquivo
    New-Item -ItemType Directory -Path (Split-Path $novo) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $origem $arquivo) -Destination $novo -Force
}
Write-Output 'Design aplicado. Assets, roteiro, API, Gradle e progresso foram preservados.'
Write-Output "Backup do código anterior: $backup"
