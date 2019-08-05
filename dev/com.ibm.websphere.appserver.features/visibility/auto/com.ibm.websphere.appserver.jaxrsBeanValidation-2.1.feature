-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsBeanValidation-2.1
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.beanValidationCore-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxrs.2.0.beanvalidation
kind=ga
edition=core
WLP-Activation-Type: parallel
