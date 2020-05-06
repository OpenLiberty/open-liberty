-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance3.0-metrics
singleton=true
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpFaultTolerance-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.3))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.microprofile.faulttolerance.3.0.metrics
kind=noship
edition=full
WLP-Activation-Type: parallel
