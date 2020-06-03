-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.activation-2.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta Activation 2.0
-bundles=\
  io.openliberty.jakarta.activation.2.0;  location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.activation:jakarta.activation-api:2.0.0-RC3"
kind=noship
edition=core
WLP-Activation-Type: parallel