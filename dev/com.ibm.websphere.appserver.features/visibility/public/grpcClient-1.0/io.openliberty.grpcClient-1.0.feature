-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc.netty; type="third-party", \
  io.netty.handler.ssl; type="third-party", \
  io.openliberty.grpc.internal.client; type="internal", \
  com.ibm.websphere.endpoint; type="ibm-api", \
  io.grpc;  type="stable"
IBM-SPI-Package: \
  com.ibm.wsspi.http, \
  com.ibm.wsspi.http.ee8
IBM-ShortName: grpcClient-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Client 1.0
-features= \
  com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="5.0, 6.0, 6.1", \
  io.openliberty.grpcClient1.0.internal.ee-8.0; ibm.tolerates:="9.0", \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  io.openliberty.internal.grpc-1.0
-jars=\
  io.openliberty.io.grpc.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.69.0", \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.69.0", \
  io.openliberty.grpc.client.1.0.thirdparty; location:="dev/api/third-party/,lib/", \
  io.openliberty.grpc.client.1.0.jakarta.thirdparty; location:="dev/api/third-party/,lib/"
-bundles=\
  io.openliberty.org.apache.commons.logging, \
  io.openliberty.io.netty, \
  io.openliberty.io.netty.ssl
kind=ga
edition=core
WLP-Activation-Type: parallel
