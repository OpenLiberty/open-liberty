-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.autoRequestProbeServlet-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestProbeServlet-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.request.probe.servlet
kind=ga
edition=core
