@ECHO OFF
SETLOCAL

SET "MAVEN_PROJECTBASEDIR=%~dp0"
IF "%MAVEN_PROJECTBASEDIR:~-1%"=="\" SET "MAVEN_PROJECTBASEDIR=%MAVEN_PROJECTBASEDIR:~0,-1%"

SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
IF NOT EXIST "%WRAPPER_JAR%" (
    ECHO Maven Wrapper JAR not found: %WRAPPER_JAR%
    EXIT /B 1
)

"%JAVA_HOME%\bin\java.exe" -version >NUL 2>&1
IF ERRORLEVEL 1 SET "JAVA_CMD=java"
IF NOT ERRORLEVEL 1 SET "JAVA_CMD=%JAVA_HOME%\bin\java.exe"

"%JAVA_CMD%" -Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
EXIT /B %ERRORLEVEL%
