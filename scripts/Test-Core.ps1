param(
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$GradleLib
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
if (!$JavaHome) {
    $localJava = Get-ChildItem -LiteralPath (Join-Path $projectRoot '.local-tools\java') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJava) { $JavaHome = $localJava.FullName }
}
if (!$GradleLib) {
    $candidate = Get-ChildItem -LiteralPath (Join-Path $env:USERPROFILE '.gradle\wrapper\dists\gradle-8.9-bin') -Filter 'kotlin-compiler-embeddable-1.9.23.jar' -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($candidate) { $GradleLib = $candidate.DirectoryName }
}
if (!$JavaHome -or !(Test-Path -LiteralPath "$JavaHome\bin\java.exe")) { throw 'Set JAVA_HOME to a JDK 17 or 21, or pass -JavaHome.' }
if (!$GradleLib) { throw 'Pass -GradleLib pointing to Gradle 8.9 lib/, or use gradlew.bat :app:testDebugUnitTest.' }
$junit = Join-Path $projectRoot '.local-tools\test-libs\junit-4.13.2.jar'
$hamcrest = Join-Path $projectRoot '.local-tools\test-libs\hamcrest-core-1.3.jar'
if (!(Test-Path -LiteralPath $junit) -or !(Test-Path -LiteralPath $hamcrest)) { throw 'Standalone test JARs missing. Use gradlew.bat :app:testDebugUnitTest to resolve JUnit normally.' }
$classpath = "$GradleLib\kotlin-stdlib-1.9.23.jar;$junit;$hamcrest"
$sourceRoot = Join-Path $projectRoot 'app\src\main\java\ink\qingji\app'
$sourceFiles = @(Get-ChildItem -LiteralPath "$sourceRoot\data" -Filter '*.kt' | Where-Object Name -ne 'TransactionDb.kt' | ForEach-Object FullName)
$sourceFiles += "$sourceRoot\notification\PaymentNotificationParser.kt"
$sourceFiles += Join-Path $projectRoot 'app\src\test\java\ink\qingji\app\RecoveryBehaviorTest.kt'
$output = Join-Path $projectRoot 'build\recovery-tests'
New-Item -ItemType Directory -Force -Path $output | Out-Null
& "$JavaHome\bin\java.exe" '-Dfile.encoding=UTF-8' -cp "$GradleLib\*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath $classpath -d "$output\tests.jar" @sourceFiles
if ($LASTEXITCODE -ne 0) { throw "Kotlin compilation failed: $LASTEXITCODE" }
& "$JavaHome\bin\java.exe" -cp "$output\tests.jar;$classpath" org.junit.runner.JUnitCore ink.qingji.app.RecoveryBehaviorTest
if ($LASTEXITCODE -ne 0) { throw "JUnit failed: $LASTEXITCODE" }
