-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakarta-jdbc-4.2
IBM-Provision-Capability:\
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.transaction-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.jdbc.jakarta,\
 com.ibm.ws.jdbc.4.1.jakarta,\
 com.ibm.ws.jdbc.4.2.jakarta
kind=noship
edition=full
