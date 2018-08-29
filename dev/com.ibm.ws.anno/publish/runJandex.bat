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

cd c:\dev\libertyws\Anno2\publish\appData\dt7.ear

%JANDEX_BIN% daytrader-ee7-ejb.jar

cd c:\dev\libertyws\Anno2\publish\appData\dt7.ear\daytrader-ee7-web.war\WEB-INF\lib

%JANDEX_BIN% asm-3.3.jar
%JANDEX_BIN% asm-commons-3.3.jar
%JANDEX_BIN% asm-tree-3.3.jar
%JANDEX_BIN% commons-fileupload-1.3.1.jar
%JANDEX_BIN% commons-io-2.2.jar
%JANDEX_BIN% commons-lang3-3.2.jar
%JANDEX_BIN% freemarker-2.3.22.jar
%JANDEX_BIN% javassist-3.11.0.GA.jar
%JANDEX_BIN% ognl-3.0.6.jar
%JANDEX_BIN% standard-1.1.1.jar
%JANDEX_BIN% struts2-convention-plugin-2.3.24.jar
%JANDEX_BIN% struts2-core-2.3.24.jar
%JANDEX_BIN% xwork-core-2.3.24.jar

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