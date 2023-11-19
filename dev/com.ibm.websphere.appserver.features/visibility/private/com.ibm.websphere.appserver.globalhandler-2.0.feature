-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.globalhandler-2.0
singleton=true
visibility=private
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-features=\
  io.openliberty.servlet.api-5.0, \
  io.openliberty.globalhandler1.0.internal.ee-9.0
-jars=\
  io.openliberty.globalhandler.spi; location:=dev/spi/ibm/
-files=\
  dev/spi/ibm/javadoc/io.openliberty.globalhandler.spi_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
