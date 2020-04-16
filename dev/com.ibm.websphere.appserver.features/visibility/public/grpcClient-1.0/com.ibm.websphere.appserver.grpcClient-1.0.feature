-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  com.ibm.ws.grpc.client; type="internal", \
  io.grpc.netty.shaded.io.grpc.netty; type="internal", \
  io.grpc.netty.shaded.io.netty.bootstrap; type="internal", \
  io.grpc.netty.shaded.io.netty.buffer; type="internal", \
  io.grpc.netty.shaded.io.netty.channel; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.embedded; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.epoll; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.group; type="internal", \
  io.grpc.netty.shaded.io.netty.channel.internal; type="internal"
IBM-ShortName: grpcClient-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Server 1.0
-features=\
  com.ibm.websphere.appserver.internal.grpc-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.servlet-4.0
-bundles=\
  com.ibm.ws.grpc.common.1.0, \
  com.ibm.ws.grpc.client.1.0, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=noship
edition=full
WLP-Activation-Type: parallel