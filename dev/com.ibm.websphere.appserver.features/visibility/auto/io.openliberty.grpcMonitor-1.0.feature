-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcMonitor-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.grpc-1.0)(osgi.identity=io.openliberty.grpcClient-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.grpc.1.0.internal.monitor
kind=ga
edition=full
WLP-Activation-Type: parallel
