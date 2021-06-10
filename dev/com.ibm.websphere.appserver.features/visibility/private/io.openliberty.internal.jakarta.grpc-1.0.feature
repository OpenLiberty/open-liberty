-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.internal.jakarta.grpc-1.0
singleton=true
IBM-API-Package: \
  com.google.common.collect; type="internal",\
  com.google.common.util.concurrent; type="internal",\
  com.google.protobuf;  type="internal",\
  io.grpc;  type="stable", \
  io.grpc.protobuf;  type="internal", \
  io.grpc.stub;  type="internal", \
  io.grpc.stub.annotations;  type="internal", \
  io.grpc.internal; type="internal"
Subsystem-Name: Jakarta gRPC internal 1.0
-bundles=\
  io.openliberty.grpc.1.0.internal.common.jakarta, \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.36.1"
-features=com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  io.openliberty.jakarta.annotation-2.0,\
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.anno-2.0, \
  com.ibm.websphere.appserver.artifact-1.0, \
  com.ibm.websphere.appserver.httptransport-1.0
kind=noship
edition=full
WLP-Activation-Type: parallel
