-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsf.beanValidation-2.2
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jsf-2.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jsf.2.2.beanvalidation
kind=ga
edition=core
