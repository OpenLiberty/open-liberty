-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidationCDI-3.0
visibility=private
IBM-App-ForceRestart: install, uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.beanvalidation.v20.cdi.jakarta,\
  com.ibm.ws.org.hibernate.validator.cdi.7.0
kind=ga
edition=core
