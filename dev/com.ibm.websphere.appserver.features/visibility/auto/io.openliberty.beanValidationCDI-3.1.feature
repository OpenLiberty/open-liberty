-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.beanValidationCDI-3.1
visibility=private
IBM-App-ForceRestart: install, uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-4.1))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.beanValidation-3.1))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.beanvalidation.v20.cdi.jakarta,\
  io.openliberty.org.hibernate.validator.cdi.9.0
kind=beta
edition=core
WLP-Activation-Type: parallel
