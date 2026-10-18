@ECHO OFF

REM Gradle Wrapper 啟動器；由 Gradle 8.9 產生。
SET DIR=%~dp0
IF "%JAVA_HOME%"=="" (
  SET JAVA=java.exe
) ELSE (
  SET JAVA=%JAVA_HOME%\bin\java.exe
)

"%JAVA%" %GRADLE_OPTS% %JAVA_OPTS% -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
