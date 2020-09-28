-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.faces.beanValidation-2.3
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.faces-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.beanValidation-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jsf.beanvalidation.jakarta
kind=noship
edition=full
