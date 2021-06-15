-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.openliberty.grpc.annotation; type="ibm-api"
IBM-ShortName: grpc-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC 1.0
-features=com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="5.0"
-files=dev/api/ibm/javadoc/io.openliberty.grpc.1.0_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
