setLocal


set JANDEX_HOME=c:\dev\repos-pub\open-liberty\dev\build.image\wlp\lib
set JANDEX_JAR=com.ibm.ws.org.jboss.jandex_1.0.21.jar
set JANDEX_PATH=%JANDEX_HOME%\%JANDEX_JAR%
echo Jandex Path [ JANDEX_PATH ] [ %JANDEX_PATH% ]
echo.

set JAVA_HOME=c:\dev\java80
set JAVA_BIN=%JAVA_HOME%\bin\java
echo Java Launch [ JAVA_BIN ] [ %JAVA_BIN% ]
echo.

set JANDEX_BIN=%JAVA_BIN% -jar %JANDEX_PATH% -m
echo Jandex Launch [ JANDEX_BIN ] [ %JANDEX_BIN% ]
echo.

cd c:\dev\libertyws\Anno2\publish\appData\AcmeAnnuityWeb.ear\AcmeAnnuityWeb.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\AppDeployBench.ear\HungryVehicle.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\dt7.ear\daytrader-ee7-web.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\ErrorTest.ear\classMismatch.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\ErrorTest.ear\infoFailures.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\ErrorTest.ear\nonValidClass.war\WEB-IN
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\ErrorTest.ear\nonValidPackage.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\ErrorTest.ear\packageMismatch.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\loginmethod.ear\metadataCompleteTrue.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\loginmethod.ear\staticAnnotationMixed.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\loginmethod.ear\staticAnnotationPure.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\loginmethod.ear\staticAnnotationWebXML.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\SCIAbsolute.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\SCIAbsoluteNoOthers.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30AnnMixed.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30AnnPure.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30AnnWebXML.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30api.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30apiFL.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30DynConflict.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30DynPure.war\WEB-INF
%JANDEX_BIN% classes

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30Inherit.war\WEB-INF
%JANDEX_BIN% classes
