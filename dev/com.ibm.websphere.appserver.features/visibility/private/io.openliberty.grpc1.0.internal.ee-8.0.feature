-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc1.0.internal.ee-8.0
singleton=true
Subsystem-Name: gRPC internal 1.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0, \ 
  com.ibm.websphere.appserver.javax.annotation-1.3; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.anno-1.0
-bundles=\
  io.openliberty.grpc.1.0.internal.common, \
  io.openliberty.io.grpc.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.38.1", \
  io.openliberty.grpc.1.0.internal, \
  com.ibm.ws.security.authorization.util
kind=ga
edition=core
WLP-Activation-Type: parallel