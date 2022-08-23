-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpFaultTolerance4.0-metrics
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpFaultTolerance-4.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpMetrics-4.0)(osgi.identity=io.openliberty.mpMetrics-5.0)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.microprofile.faulttolerance.3.0.internal.metrics
kind=ga
edition=core
WLP-Activation-Type: parallel
