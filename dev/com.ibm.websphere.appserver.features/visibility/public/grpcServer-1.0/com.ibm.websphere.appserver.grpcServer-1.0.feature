-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcServer-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc;  type="spec", \
  io.grpc.protobuf;  type="spec", \
  io.grpc.stub;  type="spec", \
  io.grpc.stub.annotations;  type="spec", \
  io.grpc.internal; type="internal",\
  com.google.protobuf;  type="internal",\
  com.google.common.util.concurrent; type="internal",\
  com.google.common.collect; type="internal"
IBM-ShortName: grpcServer-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Server 1.0
-bundles=\
 com.ibm.ws.grpc.common.1.0, \
 com.ibm.ws.grpc.server.1.0
kind=noship
edition=core
WLP-Activation-Type: parallel