-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoRequestTimingServlet-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestTiming-1.0))", \
 osgi.identity; filter:="(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.request.timing.servlet
kind=ga
edition=core
