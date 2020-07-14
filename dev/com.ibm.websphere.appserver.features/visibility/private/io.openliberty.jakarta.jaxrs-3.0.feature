-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.jaxrs-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta RESTful Web Services 3.0
-features=\
  io.openliberty.jakarta.activation-2.0
-bundles=\
  io.openliberty.jakarta.jaxrs.3.0;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ws.rs:jakarta.ws.rs-api:3.0.0-M1"
kind=noship
edition=full
WLP-Activation-Type: parallel