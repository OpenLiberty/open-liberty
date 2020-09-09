-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet5.0-requestProbeServlet1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.requestProbeServlet-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-5.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.request.probe.servlet.jakarta
kind=beta
edition=core
