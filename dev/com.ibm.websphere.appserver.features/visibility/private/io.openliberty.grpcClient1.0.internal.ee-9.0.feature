-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient1.0.internal.ee-9.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
Subsystem-Name: Jakarta gRPC Client 1.0
-features=\
  com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0"
-bundles=\
  io.openliberty.grpc.1.0.internal.common.jakarta, \
  io.openliberty.grpc.1.0.internal.client.jakarta, \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.38.1", \
  io.openliberty.grpc.client.1.0.jakarta.thirdparty; location:="dev/api/third-party/,lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
