# Whip Guardian World Breaker - build on Windows (PowerShell 5+)
# Requires JDK 21, network for Gradle/Maven, and the official RimuLib 1.3.0 jar.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$rimulib = Join-Path $PSScriptRoot 'libs\rimulib-1.3.0-neoforge.jar'
if (-not (Test-Path $rimulib)) {
  Write-Error "RimuLib 1.3.0 jar ausente: coloque o JAR oficial em $rimulib"
  exit 1
}
try { $javaVersion = & java -version 2>&1 | Out-String } catch {
  Write-Error 'Java nao encontrado. Instale o JDK 21 e tente novamente.'
  exit 1
}
if ($javaVersion -notmatch 'version "21[\.]') {
  Write-Error "Java 21 necessario. Java atual: $javaVersion"
  exit 1
}
$gradleCmd = Get-Command gradle -ErrorAction SilentlyContinue
if ($gradleCmd) {
  $gradle = $gradleCmd.Source
} else {
  $gradleDir = Join-Path $PSScriptRoot '.local-gradle\gradle-8.14'
  $gradle = Join-Path $gradleDir 'bin\gradle.bat'
  if (-not (Test-Path $gradle)) {
    $base = Join-Path $PSScriptRoot '.local-gradle'
    New-Item -ItemType Directory -Path $base -Force | Out-Null
    $zip = Join-Path $base 'gradle-8.14-bin.zip'
    Write-Host 'Baixando Gradle 8.14 da distribuicao oficial...'
    Invoke-WebRequest -Uri 'https://services.gradle.org/distributions/gradle-8.14-bin.zip' -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $base -Force
    Remove-Item $zip
  }
}
Write-Host 'Compilando Whip Guardian...'
& $gradle --no-daemon clean build
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$jar = Join-Path $PSScriptRoot 'build\libs\whip_cinematic-0.1.0.jar'
if (-not (Test-Path $jar)) {
  Write-Error 'Gradle terminou mas o JAR esperado nao foi encontrado.'
  exit 2
}
Write-Host "JAR criado: $jar"
