-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-metrics
singleton=true
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-1.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-1.1))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.microprofile.faulttolerance.metrics
kind=ga
edition=core
