-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.websocket-2.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: jakarta.websocket; version="2.1"; type="spec", \
 jakarta.websocket.server; version="2.1"; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-2.1
Subsystem-Name: Jakarta WebSocket 2.1
-features=io.openliberty.jakarta.websocket-2.1, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=com.ibm.ws.wsoc.jakarta, \
 com.ibm.ws.wsoc.2.1.jakarta, \
 io.openliberty.wsoc.2.1.internal, \
 io.openliberty.wsoc.ssl.internal, \
 io.openliberty.io.netty, \
 io.openliberty.netty.internal, \
 io.openliberty.io.netty.ssl, \
 io.openliberty.netty.internal.impl
-jars=io.openliberty.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.wsoc_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
