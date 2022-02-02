-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalHandler-2.0
visibility=public
singleton=true
IBM-ShortName: globalHandler-2.0
IBM-App-ForceRestart: uninstall, \
 install
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-features=io.openliberty.servlet.api-6.0; apiJar=false; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  io.openliberty.globalhandler1.0.internal.ee-10.0
-jars=com.ibm.websphere.appserver.spi.globalhandler; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.globalhandler_1.0-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
