-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-websocket2.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.websocket-2.0)(osgi.identity=io.openliberty.websocket-2.1)(osgi.identity=io.openliberty.websocket-2.2)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=io.openliberty.cdi-4.0)(osgi.identity=io.openliberty.cdi-4.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.wsoc.cdi.weld.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
