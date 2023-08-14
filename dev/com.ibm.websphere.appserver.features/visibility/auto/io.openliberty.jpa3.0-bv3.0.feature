-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jpa3.0-bv3.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.persistenceContainer-3.0)(osgi.identity=io.openliberty.persistenceContainer-3.1)(osgi.identity=io.openliberty.persistenceContainer-3.2)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.beanValidation-3.0)(osgi.identity=io.openliberty.beanValidation-3.1)))"
-bundles=com.ibm.ws.jpa.container.beanvalidation.2.0.jakarta
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
