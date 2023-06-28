-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet5.0-monitor1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-5.0)(osgi.identity=io.openliberty.servlet.internal-6.0)(osgi.identity=io.openliberty.servlet.internal-6.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.webcontainer.monitor.jakarta; start-phase:=APPLICATION_EARLY
kind=ga
edition=core
