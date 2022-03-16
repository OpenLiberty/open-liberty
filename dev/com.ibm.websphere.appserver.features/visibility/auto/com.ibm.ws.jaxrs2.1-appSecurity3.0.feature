-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.ws.jaxrs2.1-appSecurity3.0
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxrs-2.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))"
-bundles=com.ibm.ws.jaxrs.2.1.appSecurity, \
 com.ibm.ws.security.authorization.util
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
