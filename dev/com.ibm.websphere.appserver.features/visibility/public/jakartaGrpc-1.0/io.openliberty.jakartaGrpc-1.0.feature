-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaGrpc-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.openliberty.grpc.annotation; type="ibm-api"
IBM-ShortName: jakartaGrpc-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC 1.0
-bundles=\
  io.openliberty.grpc.1.0.internal.jakarta,\
  com.ibm.ws.security.authorization.util.jakarta
-features=com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.internal.jakarta.grpc-1.0
-jars=\
  io.openliberty.grpc.1.0.jakarta; location:="dev/api/ibm/,lib/"
-files=dev/api/ibm/javadoc/io.openliberty.grpc.1.0_1.0-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
