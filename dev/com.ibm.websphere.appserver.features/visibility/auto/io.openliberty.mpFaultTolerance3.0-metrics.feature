-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance3.0-metrics
singleton=true
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpFaultTolerance-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpMetrics-3.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.microprofile.faulttolerance.3.0.internal.metrics
kind=beta
edition=core
WLP-Activation-Type: parallel
