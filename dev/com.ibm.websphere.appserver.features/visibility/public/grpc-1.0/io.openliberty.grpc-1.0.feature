-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.grpc-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
  uninstall
IBM-API-Package: \
  io.openliberty.grpc.annotation; type="ibm-api", \
  com.ibm.websphere.endpoint; type="ibm-api", \
  io.grpc;  type="stable"
IBM-SPI-Package: \
  com.ibm.wsspi.http, \
  com.ibm.wsspi.http.ee8
IBM-ShortName: grpc-1.0
Subsystem-Version: 1.0.0
Subsystem-Name: gRPC 1.0
-features=\
  com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="5.0,6.0,6.1", \
  io.openliberty.grpc1.0.internal.ee-8.0; ibm.tolerates:="9.0", \
  io.openliberty.internal.grpc-1.0
-files=dev/api/ibm/javadoc/io.openliberty.grpc.1.0_1.0-javadoc.zip
-jars=\
  io.openliberty.grpc.1.0; location:="dev/api/ibm/,lib/", \
  io.openliberty.io.grpc.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.69.0", \
  io.openliberty.io.grpc.1.0.jakarta; location:="dev/api/stable/,lib/"; mavenCoordinates="io.grpc:grpc-api:1.69.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
