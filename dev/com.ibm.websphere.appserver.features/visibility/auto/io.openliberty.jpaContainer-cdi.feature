-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jpaContainer-cdi
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jpaContainer-3.0))"
-bundles=com.ibm.ws.jpa.container.v21.cdi.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
