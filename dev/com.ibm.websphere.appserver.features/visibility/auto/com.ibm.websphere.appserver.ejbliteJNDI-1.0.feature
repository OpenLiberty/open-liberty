-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbliteJNDI-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.ejbLiteCore-1.0)(osgi.identity=io.openliberty.ejbLiteCore-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jndi-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jndi.ejb
kind=ga
edition=core
WLP-Activation-Type: parallel
