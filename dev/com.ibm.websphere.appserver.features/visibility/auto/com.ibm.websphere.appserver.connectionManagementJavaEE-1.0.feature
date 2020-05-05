-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionManagementJavaEE-1.0
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-1.2)(osgi.identity=com.ibm.websphere.appserver.transaction-1.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.connectionManagement-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.javax.connector.internal-1.6; ibm.tolerates:=1.7
-bundles=com.ibm.ws.jca.cm
kind=ga
edition=core
