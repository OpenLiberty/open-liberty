-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient1.0.internal.ee-8.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Client 1.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0, \ 
  com.ibm.websphere.appserver.javax.annotation-1.3; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.anno-1.0
-bundles=\
  io.openliberty.grpc.1.0.internal.client, \
  io.openliberty.grpc.client.1.0.thirdparty; location:="dev/api/third-party/,lib/", \
  io.openliberty.io.grpc.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.38.1", \
  io.openliberty.grpc.1.0.internal.common
kind=ga
edition=core
WLP-Activation-Type: parallel