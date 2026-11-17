[CmdletBinding()]
param([switch]$SkipBuild)

$ErrorActionPreference = 'Stop'
$javaCommand = (Get-Command java -ErrorAction Stop).Source
$wrapperArgs = @(
    '-Dmaven.multiModuleProjectDirectory=.',
    '-classpath', '.mvn/wrapper/maven-wrapper.jar',
    'org.apache.maven.wrapper.MavenWrapperMain', '-B', '-ntp'
)

function Invoke-DemoMaven {
    param([string[]]$Goals)
    & $javaCommand @wrapperArgs @Goals
    if ($LASTEXITCODE -ne 0) {
        throw "Maven failed (exit $LASTEXITCODE). The demo was not started."
    }
}

Push-Location -LiteralPath $PSScriptRoot
try {
    if (-not $SkipBuild) {
        Invoke-DemoMaven -Goals @('-pl', 'auction-client', '-am', 'install', '-DskipTests')
        Invoke-DemoMaven -Goals @(
            '-pl', 'auction-client',
            'org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath',
            '-DincludeScope=runtime', '-Dmdep.outputFile=target/runtime-classpath.txt'
        )
    }

    $dependencyFile = 'auction-client/target/runtime-classpath.txt'
    $launcherFile = 'auction-client/target/classes/com/auction/app/client/Launcher.class'
    if (-not (Test-Path -LiteralPath $dependencyFile) -or -not (Test-Path -LiteralPath $launcherFile)) {
        throw 'Build output is missing. Run .\run-demo.ps1 without -SkipBuild first.'
    }

    $dependencies = (Get-Content -LiteralPath $dependencyFile -Raw).Trim().Split([IO.Path]::PathSeparator)
    $javafxJars = @($dependencies | Where-Object { [IO.Path]::GetFileName($_) -like 'javafx-*-win.jar' })
    $otherJars = @($dependencies | Where-Object { [IO.Path]::GetFileName($_) -notlike 'javafx-*.jar' })
    if ($javafxJars.Count -eq 0) {
        throw 'Windows JavaFX runtime jars were not resolved by Maven.'
    }

    # Keep the application path relative: JavaFX Maven plugin 0.0.8 loses Vietnamese
    # characters in absolute child-process arguments on Windows with native Cp1252.
    $modulePath = $javafxJars -join [IO.Path]::PathSeparator
    $classPath = (@('target/classes') + $otherJars) -join [IO.Path]::PathSeparator
    Push-Location -LiteralPath auction-client
    try {
        Write-Host 'Opening Online Auction System. Demo login: demo / demo.'
        & $javaCommand '--module-path' $modulePath '--add-modules' 'javafx.controls,javafx.fxml' `
            '--enable-native-access=javafx.graphics' '-classpath' $classPath 'com.auction.app.client.Launcher'
        if ($LASTEXITCODE -ne 0) {
            throw "Demo exited with error $LASTEXITCODE. See the Java message above."
        }
    } finally {
        Pop-Location
    }
} finally {
    Pop-Location
}
