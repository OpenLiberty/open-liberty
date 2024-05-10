-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.httpStat-monitor1.0.jakarta
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.pages-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-6.0)(osgi.identity=com.ibm.websphere.appserver.servlet-6.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.http.monitor
kind=noship
edition=core