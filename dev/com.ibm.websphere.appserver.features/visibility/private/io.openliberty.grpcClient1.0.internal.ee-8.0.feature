-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient1.0.internal.ee-8.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Client 1.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0
-bundles=\
  io.openliberty.grpc.1.0.internal.common, \
  com.ibm.ws.com.google.guava, \
  io.openliberty.grpc.1.0.internal.client
kind=ga
edition=core
WLP-Activation-Type: parallel
