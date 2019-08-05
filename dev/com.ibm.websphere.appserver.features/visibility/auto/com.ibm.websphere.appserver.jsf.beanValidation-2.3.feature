-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jsf.beanValidation-2.3
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jsf-2.3))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidation-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jsf.beanvalidation
kind=ga
edition=core
WLP-Activation-Type: parallel
