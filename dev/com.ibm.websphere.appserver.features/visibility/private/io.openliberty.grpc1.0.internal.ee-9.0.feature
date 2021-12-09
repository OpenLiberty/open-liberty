-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc1.0.internal.ee-9.0
singleton=true
Subsystem-Name: Jakarta gRPC internal 1.0
-features=\
  com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0"
-bundles=\
  io.openliberty.grpc.1.0.internal.common.jakarta, \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.38.1", \
  io.openliberty.grpc.1.0.internal.jakarta, \
  com.ibm.ws.security.authorization.util.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
