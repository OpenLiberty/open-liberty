-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.orbJ2eeManagement-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.iioptransport-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.j2eeManagement-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.transport.iiop.management.j2ee
kind=ga
edition=base
WLP-Activation-Type: parallel
