-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaJ2eeManagement-1.0
singleton=true
IBM-Provision-Capability: osgi.identity;filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jca-1.7))", \
 osgi.identity;filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.j2eeManagement-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jca.management.j2ee
kind=ga
edition=base
WLP-Activation-Type: parallel
