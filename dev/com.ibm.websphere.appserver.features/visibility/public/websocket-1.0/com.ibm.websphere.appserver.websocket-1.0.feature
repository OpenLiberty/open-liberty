-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.websocket-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.websocket; type="spec", \
 javax.websocket.server; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-1.0
Subsystem-Name: Java WebSocket 1.0
-features= com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
 io.openliberty.javaee.websocket-1.0, \
 com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0,8.0"
-bundles=com.ibm.ws.wsoc, \
 io.openliberty.wsoc.1.0.internal
-jars=com.ibm.websphere.appserver.api.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.wsoc_1.0-javadoc.zip
kind=ga
edition=core
