-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient1.0.internal.ee-9.0
visibility=private
singleton=true
Subsystem-Version: 1.0.0
Subsystem-Name: Jakarta gRPC Client 1.0
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \ 
  io.openliberty.jakarta.annotation-2.0, \
  com.ibm.websphere.appserver.anno-2.0
-bundles=\
  io.openliberty.grpc.1.0.internal.client.jakarta, \
  io.openliberty.grpc.client.1.0.thirdparty.jakarta; location:="dev/api/third-party/,lib/", \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.38.1", \
  io.openliberty.grpc.1.0.internal.common.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel