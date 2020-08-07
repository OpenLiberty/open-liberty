-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta-connectionManagement-1.0
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.connectionManagement-1.0))"
IBM-Install-Policy: when-satisfied
-features=io.openliberty.jakarta.internal.api.connectors-2.0
-bundles=com.ibm.ws.jca.cm.jakarta
kind=beta
edition=core