-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.websocket-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.websocket; version="1.1"; type="spec", \
 javax.websocket.server; version="1.1"; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-1.1
Subsystem-Name: Java WebSocket 1.1
-features= com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
 io.openliberty.servlet.internal-3.1; ibm.tolerates:="4.0", \
 io.openliberty.javaee.websocket-1.1, \
 com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0,8.0"
-bundles=com.ibm.ws.wsoc, \
 com.ibm.ws.wsoc.1.1, \
 io.openliberty.wsoc.ssl.internal, \
 io.openliberty.io.netty, \
 io.openliberty.io.netty.ssl, \
 io.openliberty.netty.internal, \
 io.openliberty.netty.internal.impl
-jars=com.ibm.websphere.appserver.api.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.wsoc_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: javaee-7.0,javaee-8.0,jakartaee-8.0
