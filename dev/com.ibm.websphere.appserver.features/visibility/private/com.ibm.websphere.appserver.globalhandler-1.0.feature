-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.globalhandler-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-features=\
  io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0,5.0"; apiJar=false, \
  io.openliberty.globalhandler1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-jars=com.ibm.websphere.appserver.spi.globalhandler; location:=dev/spi/ibm/, \
  com.ibm.websphere.appserver.spi.globalhandler.jakarta; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.globalhandler_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
