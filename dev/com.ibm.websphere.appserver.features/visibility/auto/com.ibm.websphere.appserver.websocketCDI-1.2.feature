-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.websocketCDI-1.2
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.websocket-1.0)(osgi.identity=com.ibm.websphere.appserver.websocket-1.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.cdi-1.2)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.wsoc.cdi.weld
kind=ga
edition=core
WLP-Activation-Type: parallel
