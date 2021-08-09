@echo off
setlocal

REM We need a JVM
if not defined JAVA_HOME  (
  echo Error: JAVA_HOME is not defined.
  exit /b
)

if not defined JAVACMD (
  set JAVACMD="%JAVA_HOME%\bin\java.exe"
)

if not defined IDP_BASE_URL (
  set IDP_BASE_URL="http://localhost/idp"
)

if not exist %JAVACMD% (
  echo Error: JAVA_HOME is not defined correctly.
  echo Cannot execute %JAVACMD%
  exit /b
)

REM add in the dependency .jar files
set LOCALCLASSPATH=%~dp0lib\*;%~dp0..\webapp\WEB-INF\lib\*;%JAVA_HOME%\lib\classes.zip;%CLASSPATH%

REM Go to it !

%JAVACMD% -cp "%LOCALCLASSPATH%" -Dnet.shibboleth.idp.cli.baseURL=%IDP_BASE_URL% %*
