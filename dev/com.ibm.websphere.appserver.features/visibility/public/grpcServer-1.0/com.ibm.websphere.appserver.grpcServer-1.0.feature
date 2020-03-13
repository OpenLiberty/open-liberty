-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.grpcServer-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  com.google.common.util.concurrent; type="internal",\
  com.google.common.collect; type="internal"
IBM-ShortName: grpcServer-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC Server 1.0
-features=\
  com.ibm.websphere.appserver.channelfw-1.0,\
  com.ibm.websphere.appserver.internal.grpc-1.0,\
  com.ibm.websphere.appserver.servlet-4.0
-bundles=\
  com.ibm.ws.grpc.server.1.0
kind=noship
edition=full
WLP-Activation-Type: parallel