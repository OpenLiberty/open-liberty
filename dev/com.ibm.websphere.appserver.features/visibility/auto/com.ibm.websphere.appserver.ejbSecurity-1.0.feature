-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbSecurity-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbCore-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0)(osgi.identity=com.ibm.websphere.appserver.appSecurity-3.0)))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.ws.security.appbnd, \
 com.ibm.ws.ejbcontainer.security
kind=ga
edition=core
WLP-Activation-Type: parallel
