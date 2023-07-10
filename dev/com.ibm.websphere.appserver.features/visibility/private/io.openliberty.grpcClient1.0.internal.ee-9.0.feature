-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient1.0.internal.ee-9.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
Subsystem-Name: Jakarta gRPC Client 1.0
-features=\
  com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0, 6.1"
-bundles=\
  io.openliberty.grpc.1.0.internal.common.jakarta, \
  io.openliberty.grpc.1.0.internal.client.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
