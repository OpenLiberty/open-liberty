-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler-1.0
visibility=protected
IBM-App-ForceRestart: uninstall, \
 install
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-features=\
  io.openliberty.globalhandler1.0.internal.ee-10.0, \
  com.ibm.websphere.appserver.servlet-6.0
-jars=\
  io.openliberty.globalhandler.spi; location:=dev/spi/ibm/
-files=\
  dev/spi/ibm/javadoc/io.openliberty.globalhandler.spi_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
