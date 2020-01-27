-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpMetrics-2.3-monitor-1.0
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.3))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.monitor-1.0)))"
-bundles=com.ibm.ws.microprofile.metrics.2.3.monitor
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
