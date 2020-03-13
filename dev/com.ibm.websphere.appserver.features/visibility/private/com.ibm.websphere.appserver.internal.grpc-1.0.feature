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
-bundles=\
  com.ibm.ws.grpc.common.1.0
-features=\
  com.ibm.websphere.appserver.artifact-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.anno-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0,\
  com.ibm.websphere.appserver.httptransport-1.0, \
  com.ibm.websphere.appserver.javax.annotation-1.3; ibm.tolerates:=1.2
kind=noship
edition=full
WLP-Activation-Type: parallel