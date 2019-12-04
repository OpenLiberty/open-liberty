-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.grpc.netty.shaded.io.grpc.netty; type="stable", \
  io.grpc.netty.shaded.io.netty.bootstrap; type="stable", \
  io.grpc.netty.shaded.io.netty.buffer; type="stable", \
  io.grpc.netty.shaded.io.netty.channel; type="stable", \
  io.grpc.netty.shaded.io.netty.channel.embedded; type="stable", \
  io.grpc.netty.shaded.io.netty.channel.epoll; type="stable", \
  io.grpc.netty.shaded.io.netty.channel.group; type="stable", \
  io.grpc.netty.shaded.io.netty.channel.internal; type="stable", \
  io.grpc;  type="stable", \
  io.grpc.protobuf;  type="stable", \
  io.grpc.stub;  type="stable", \
  io.grpc.stub.annotations;  type="stable", \
  io.grpc.servlet;  type="stable", \
  com.google.protobuf;  type="stable"
IBM-ShortName: grpcClient-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Server 1.0
-bundles=\
 com.ibm.ws.grpc.common.1.0, \
 com.ibm.ws.grpc.client.1.0, \
 com.ibm.ws.org.apache.commons.logging.1.0.3
kind=noship
edition=core
WLP-Activation-Type: parallel