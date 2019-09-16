-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-metrics2.0
singleton=true
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-1.1)(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-2.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpFT-Metrics-2.0-guard))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.microprofile.faulttolerance.metrics,com.ibm.ws.microprofile.faulttolerance.metrics.2.0
kind=noship
edition=full
