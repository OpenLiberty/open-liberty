-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.websocket-2.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.websocket; version="2.0"; type="spec", \
 jakarta.websocket.server; version="2.0"; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-2.0
Subsystem-Name: Jakarta WebSocket 2.0
-features=io.openliberty.jakarta.websocket-2.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.servlet.internal-5.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.wsoc.jakarta, \
 com.ibm.ws.wsoc.2.0.jakarta, \
 io.openliberty.wsoc.ssl.internal,\
 io.openliberty.io.netty,\
 io.openliberty.io.netty.ssl,\
 io.openliberty.netty.internal,\
 io.openliberty.netty.internal.impl
-jars=io.openliberty.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.wsoc_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: jakartaee-9.1
