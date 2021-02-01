-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-xmlws3.0
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.xmlWS-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.jndi-1.0
-bundles=com.ibm.ws.jaxws.cdi.jakarta
kind=beta
edition=base
WLP-Activation-Type: parallel
