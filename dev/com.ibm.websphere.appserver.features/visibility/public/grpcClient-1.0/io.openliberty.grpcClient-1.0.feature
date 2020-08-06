-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpcClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.openliberty.grpc.internal.client; type="internal", \
  io.grpc.netty.shaded.io.grpc.netty; type="internal", \
  io.grpc.netty.shaded.io.netty.bootstrap; type="internal", \
  io.grpc.netty.shaded.io.netty.buffer; type="internal", \
  io.grpc.netty.shaded.io.netty.channel; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.embedded; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.epoll; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.group; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.internal; type="internal", \
  io.grpc.netty.shaded.io.netty.handler.ssl; type="internal"
IBM-ShortName: grpcClient-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Client 1.0
-features=\
  io.openliberty.internal.grpc-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.servlet-4.0
-bundles=\
  io.openliberty.grpc.1.0.internal.common, \
  io.openliberty.grpc.1.0.internal.client, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=beta
edition=full
WLP-Activation-Type: parallel
