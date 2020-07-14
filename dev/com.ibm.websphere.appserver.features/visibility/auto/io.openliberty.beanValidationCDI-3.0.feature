-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.beanValidationCDI-3.0
visibility=private
IBM-App-ForceRestart: install, uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.beanValidation-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.beanvalidation.v20.cdi.jakarta,\
  io.openliberty.org.hibernate.validator.cdi.7.0
kind=noship
edition=full
