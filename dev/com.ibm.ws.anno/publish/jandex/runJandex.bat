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

cd c:\dev\libertyws\Anno2\publish\appData\AcmeAnnuityWeb.ear\AcmeAnnuityWeb.war\WEB-INF\lib

%JANDEX_BIN% AcmeAnnuityCommon.jar
%JANDEX_BIN% AcmeCommon.jar

cd c:\dev\libertyws\Anno2\publish\appData\AcmeAnnuityWeb.ear\AcmeAnnuityWeb.war.manifest.jars

%JANDEX_BIN% AcmeAnnuityCommon.jar
%JANDEX_BIN% AcmeAnnuityEJB2xJAXRPCStubs.jar
%JANDEX_BIN% AcmeAnnuityEJB2xStubs.jar
%JANDEX_BIN% AcmeAnnuityEJB3JAXRPCStubs.jar
%JANDEX_BIN% AcmeAnnuityEJB3JAXWStubs.jar
%JANDEX_BIN% AcmeAnnuityEJB3Stubs.jar
%JANDEX_BIN% AcmeCommon.jar

cd c:\dev\libertyws\Anno2\publish\appData\AppDeployBench.ear

%JANDEX_BIN% GarageSaleUtils.jar
%JANDEX_BIN% GSEJB.jar

cd c:\dev\libertyws\Anno2\publish\appData\JPATest.app_1.0.0.201111251517.eba

%JANDEX_BIN% JPATest_1.0.0.201111251517.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\SCIAbsolute.war\WEB-INF\lib

%JANDEX_BIN% SCI4.jar
%JANDEX_BIN% SCIWithListener.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\SCIAbsoluteNoOthers.war\WEB-INF\lib

%JANDEX_BIN% SCI4.jar
%JANDEX_BIN% SCIWithListener.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear

%JANDEX_BIN% SecFVTS1EJB.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30AnnMixed.war\WEB-INF\lib

%JANDEX_BIN% Servlet30AnnMixed.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30AnnPure.war\WEB-INF\lib

%JANDEX_BIN% Servlet30AnnPureAltLoc.jar

cd c:\dev\libertyws\Anno2\publish\appData\SCITest.ear\secfvt_servlet30.ear\Servlet30DynConflict.war\WEB-INF\lib

%JANDEX_BIN% Servlet30DynConflict.jar

endLocal