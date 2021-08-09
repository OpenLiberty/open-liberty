-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.activation-2.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, install
IBM-Process-Types: client, server
Subsystem-Name: Jakarta Activation 2.0
IBM-API-Package: \
 jakarta.activation; type="spec"
-features=com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  io.openliberty.jakarta.activation.2.0;  location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.activation:jakarta.activation-api:2.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
