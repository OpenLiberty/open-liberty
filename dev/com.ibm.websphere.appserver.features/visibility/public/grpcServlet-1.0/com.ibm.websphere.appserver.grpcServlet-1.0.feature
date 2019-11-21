-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcServlet-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc;  type="internal", \
  io.grpc.protobuf;  type="internal", \
  io.grpc.stub;  type="internal", \
  io.grpc.stub.annotations;  type="internal", \
  io.grpc.servlet;  type="internal", \
  com.google.protobuf;  type="internal"
IBM-ShortName: grpcServlet-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Servlet 1.0
-bundles=\
 com.ibm.ws.grpc.common.1.0, \
 com.ibm.ws.grpc.servlet.1.0
-features=\
 com.ibm.websphere.appserver.servlet-4.0
kind=noship
edition=core
WLP-Activation-Type: parallel