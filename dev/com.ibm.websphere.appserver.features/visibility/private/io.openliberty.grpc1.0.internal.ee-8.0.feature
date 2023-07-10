-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc1.0.internal.ee-8.0
singleton=true
Subsystem-Name: gRPC internal 1.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0
-bundles=\
  io.openliberty.grpc.1.0.internal.common, \
  io.openliberty.grpc.1.0.internal, \
  com.ibm.ws.security.authorization.util
kind=ga
edition=core
WLP-Activation-Type: parallel
