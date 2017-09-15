-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.websocket-1.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.websocket; version="1.1"; type="spec", \
 javax.websocket.server; version="1.1"; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-1.1
Subsystem-Name: Java WebSocket 1.1
-features=com.ibm.websphere.appserver.servlet-3.1
-bundles=com.ibm.ws.wsoc, \
 com.ibm.websphere.javaee.websocket.1.1; location:="dev/api/spec/,lib/", \
 com.ibm.ws.wsoc.1.1
-jars=com.ibm.websphere.appserver.api.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.wsoc_1.0-javadoc.zip
kind=ga
edition=core
