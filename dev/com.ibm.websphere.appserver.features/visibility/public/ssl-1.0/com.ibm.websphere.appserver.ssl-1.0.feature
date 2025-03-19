-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ssl-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: ssl-1.0
IBM-API-Package: com.ibm.websphere.ssl; type="ibm-api", \
  com.ibm.websphere.endpoint; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.ssl
IBM-Process-Types: server, \
 client
Subsystem-Name: Secure Socket Layer 1.0
-features=com.ibm.websphere.appserver.channelfw-1.0, \
  com.ibm.websphere.appserver.certificateCreator-1.0; ibm.tolerates:="2.0"
-bundles=com.ibm.ws.ssl, \
 com.ibm.ws.channel.ssl, \
 com.ibm.websphere.security, \
 com.ibm.ws.crypto.certificateutil, \
 io.openliberty.wsoc.ssl.internal, \
 io.openliberty.endpoint, \
 io.openliberty.io.netty, \
 io.openliberty.io.netty.ssl, \
 io.openliberty.netty.internal, \
 io.openliberty.netty.internal.tls.impl
-jars=com.ibm.websphere.appserver.spi.ssl; location:=dev/spi/ibm/, \
 com.ibm.websphere.appserver.api.ssl; location:=dev/api/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.ssl_1.6-javadoc.zip, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.ssl_1.8-javadoc.zip
kind=ga
edition=core
superseded-by=transportSecurity-1.0
WLP-InstantOn-Enabled: true
