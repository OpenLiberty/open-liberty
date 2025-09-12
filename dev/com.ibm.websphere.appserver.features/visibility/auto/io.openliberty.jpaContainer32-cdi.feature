-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jpaContainer32-cdi
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(osgi.identity=io.openliberty.cdi-4.1)", \
  osgi.identity; filter:="(osgi.identity=io.openliberty.persistenceContainer-3.2)"
-bundles=com.ibm.ws.jpa.container.v32.cdi
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
WLP-Activation-Type: parallel
