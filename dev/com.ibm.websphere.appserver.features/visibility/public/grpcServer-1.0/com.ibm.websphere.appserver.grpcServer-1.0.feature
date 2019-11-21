-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcServer-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc;  type="stable", \
  io.grpc.protobuf;  type="stable", \
  io.grpc.stub;  type="stable", \
  io.grpc.stub.annotations;  type="stable", \
  io.grpc.servlet;  type="stable", \
  com.google.protobuf;  type="stable"
IBM-ShortName: grpcServer-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Server 1.0
-bundles=\
 com.ibm.ws.grpc.common.1.0, \
 com.ibm.ws.grpc.server.1.0
-features=\
 com.ibm.websphere.appserver.servlet-4.0
kind=noship
edition=core
WLP-Activation-Type: parallel