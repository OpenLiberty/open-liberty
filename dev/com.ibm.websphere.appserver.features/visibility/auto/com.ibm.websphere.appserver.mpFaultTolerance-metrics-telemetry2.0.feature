-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpFaultTolerance-telemetry-metrics2.0
IBM-Provision-Capability: \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpFaultTolerance-4.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpTelemetry-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.microprofile.faulttolerance.telemetry, \
 io.openliberty.microprofile.faulttolerance.4.0.telemetry.metrics.2.0
kind=ga
edition=core
