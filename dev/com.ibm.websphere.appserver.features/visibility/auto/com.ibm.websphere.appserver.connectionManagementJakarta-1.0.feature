-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionManagementJakarta-1.0
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.connectionManagement-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.javax.connector.internal-1.7
-bundles=com.ibm.ws.jca.cm.jakarta
kind=noship
edition=core
#TODO switch feature to Jakarta Connectors once available
