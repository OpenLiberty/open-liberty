-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-metrics2.0
singleton=true
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-1.1)(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-2.0)(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-2.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.0)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.2)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.3)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.microprofile.faulttolerance.metrics,com.ibm.ws.microprofile.faulttolerance.metrics.2.0
kind=ga
edition=core
