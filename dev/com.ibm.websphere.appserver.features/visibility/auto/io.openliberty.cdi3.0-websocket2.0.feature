-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-websocket2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.websocket-2.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.wsoc.cdi.weld.jakarta
kind=noship
edition=core
WLP-Activation-Type: parallel
