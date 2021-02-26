@echo off
setlocal

REM Find the necessary resources
set ANT_HOME=%~dp0

REM strip trailing backslash - it confuses java.
REM do it like this because embedding the strip inside an if causes problems if the name has a ")" in it. 
if "%ANT_HOME:~-1%" NEQ "\" (
   goto nostrip
)
set ANT_HOME=%ANT_HOME:~0,-1%
:nostrip

REM We need a JVM
if not defined JAVA_HOME  (
  echo Error: JAVA_HOME is not defined.
  exit /b
)

if not defined JAVACMD (
  set JAVACMD="%JAVA_HOME%\bin\java.exe"
)

if not exist %JAVACMD% (
  echo Error: JAVA_HOME is not defined correctly.
  echo Cannot execute %JAVACMD%
  exit /b
)

REM add in the dependency .jar files
set LOCALCLASSPATH=%ANT_HOME%\..\bin\lib\*;%ANT_HOME%\..\webapp\WEB-INF\lib\*;%JAVA_HOME%\lib\tools.jar;%JAVA_HOME%\lib\classes.zip;%CLASSPATH%

%JAVACMD% -cp "%LOCALCLASSPATH%" -Dant.home="%ANT_HOME%" %ANT_OPTS% org.apache.tools.ant.Main -e -q -f "%ANT_HOME%/build.xml" %*