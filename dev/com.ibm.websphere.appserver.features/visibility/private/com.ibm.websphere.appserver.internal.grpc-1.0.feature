-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.grpc-1.0
singleton=true
IBM-API-Package: \
  com.google.protobuf;  type="stable",\
  io.grpc;  type="stable", \
  io.grpc.protobuf;  type="stable", \
  io.grpc.stub;  type="stable", \
  io.grpc.stub.annotations;  type="stable", \
  io.grpc.internal; type="internal",\
Subsystem-Name: gRPC internal 1.0
-bundles= com.ibm.ws.grpc.common.1.0
kind=noship
edition=core
