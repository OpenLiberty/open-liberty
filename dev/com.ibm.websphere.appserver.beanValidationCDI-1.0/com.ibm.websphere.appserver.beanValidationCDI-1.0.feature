-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.beanValidationCDI-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.cdi-1.2)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.beanValidation-1.1)(osgi.identity=com.ibm.websphere.appserver.beanValidation-2.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.beanvalidation.v11.cdi
kind=ga
edition=core
