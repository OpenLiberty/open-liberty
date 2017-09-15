-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.websocket-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.websocket; type="spec", \
 javax.websocket.server; type="spec", \
 com.ibm.websphere.wsoc; type="ibm-api"
IBM-ShortName: websocket-1.0
Subsystem-Name: Java WebSocket 1.0
-features=com.ibm.websphere.appserver.servlet-3.1
-bundles=com.ibm.ws.wsoc, \
 com.ibm.websphere.javaee.websocket.1.0; location:="dev/api/spec/,lib/"
-jars=com.ibm.websphere.appserver.api.wsoc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.wsoc_1.0-javadoc.zip
kind=ga
edition=core
