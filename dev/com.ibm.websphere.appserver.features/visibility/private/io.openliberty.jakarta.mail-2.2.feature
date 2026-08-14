-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.mail-2.2
visibility=private
singleton=true
Subsystem-Version: 2.2
IBM-Process-Types: client, \
 server
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jakarta.activation-2.2, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.jakarta.mail.2.1;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.mail:jakarta.mail-api:2.1.1"
kind=noship
edition=full
WLP-Activation-Type: parallel
