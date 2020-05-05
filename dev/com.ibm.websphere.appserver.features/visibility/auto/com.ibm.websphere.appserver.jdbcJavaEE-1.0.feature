-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jdbcJavaEE-1.0
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-1.2)(osgi.identity=com.ibm.websphere.appserver.transaction-1.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jdbc-4.3)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.1)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jdbc
kind=ga
edition=core
