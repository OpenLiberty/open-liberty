-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidationCDI-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.beanvalidation.v11.cdi
kind=ga
edition=core
