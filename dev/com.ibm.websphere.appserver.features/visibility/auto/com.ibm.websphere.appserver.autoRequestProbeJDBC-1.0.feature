-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoRequestProbeJDBC-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestProbeJDBC-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jdbc-4.0)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.1)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.3)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.request.probe.jdbc
kind=ga
edition=core
