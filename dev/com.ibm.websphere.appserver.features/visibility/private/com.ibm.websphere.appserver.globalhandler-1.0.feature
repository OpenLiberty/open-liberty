-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.globalhandler-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-bundles=com.ibm.ws.webservices.handler
-jars=com.ibm.websphere.appserver.spi.globalhandler; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.globalhandler_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
