-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
-bundles=io.openliberty.webservices.handler.2.0
-jars=com.ibm.websphere.appserver.spi.globalhandler.jakarta; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.globalhandler_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
