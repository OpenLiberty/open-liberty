setLocal

set JANDEX_HOME=c:\dev\repos-pub\open-liberty\dev\build.image\wlp\lib
set JANDEX_JAR=com.ibm.ws.org.jboss.jandex_1.0.21.jar
set JANDEX_PATH=%JANDEX_HOME%\%JANDEX_JAR%
echo Jandex Path [ JANDEX_PATH ] [ %JANDEX_PATH% ]
echo.

set JAVA_HOME=c:\dev\java80
set JAVA_BIN=%JAVA_HOME%\bin\java
echo Java Binaries [ JAVA_BIN ] [ %JAVA_BIN% ]
echo.

%JAVA_BIN% -jar %JANDEX_PATH%

endLocal