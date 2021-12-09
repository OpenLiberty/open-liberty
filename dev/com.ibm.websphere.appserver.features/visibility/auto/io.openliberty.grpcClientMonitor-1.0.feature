-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClientMonitor-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.grpcClient-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.grpc.1.0.internal.client.monitor
kind=ga
edition=core
WLP-Activation-Type: parallel
