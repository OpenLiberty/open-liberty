-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jpaContainer40-cdi
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-4.1)(osgi.identity=io.openliberty.cdi-5.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.persistenceContainer-4.0))"
-bundles=io.openliberty.jpa.container.4.0.cdi
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
WLP-Activation-Type: parallel
