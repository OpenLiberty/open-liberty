-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaGrpcClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc.netty; type="third-party", \
  io.netty.handler.ssl; type="third-party", \
  io.openliberty.grpc.internal.client; type="internal"
IBM-ShortName: jakartaGrpcClient-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: Jakarta gRPC Client 1.0
-features=com.ibm.websphere.appserver.servlet-5.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  io.openliberty.internal.jakarta.grpc-1.0
-bundles=\
  io.openliberty.grpc.1.0.internal.client.jakarta, \
  io.openliberty.grpc.client.1.0.thirdparty.jakarta; location:="dev/api/third-party/,lib/", \
  io.openliberty.org.apache.commons.logging
kind=noship
edition=full
WLP-Activation-Type: parallel
