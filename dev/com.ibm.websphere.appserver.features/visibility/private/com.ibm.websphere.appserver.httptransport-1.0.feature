-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.httptransport-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-SPI-Package: com.ibm.wsspi.http, \
com.ibm.wsspi.http.ee8
Subsystem-Version: 1.0
-features=com.ibm.websphere.appserver.channelfw-1.0
-bundles=com.ibm.ws.transport.http, \
io.openliberty.io.netty,\
io.openliberty.io.netty.ssl,\
io.openliberty.netty.internal,\
io.openliberty.netty.internal.impl,\
io.openliberty.endpoint
-jars=com.ibm.websphere.appserver.spi.httptransport; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.httptransport_4.2-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
