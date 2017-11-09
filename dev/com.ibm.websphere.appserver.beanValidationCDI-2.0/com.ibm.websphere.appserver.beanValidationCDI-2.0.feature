-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidationCDI-2.0
visibility=private
IBM-App-ForceRestart: install, uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.org.hibernate.validator.cdi.6.0.4.Final
kind=beta
edition=core
