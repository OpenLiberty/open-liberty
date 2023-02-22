-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.mail-2.1
visibility=private
singleton=true
Subsystem-Version: 2.1
IBM-Process-Types: client, \
 server
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.activation-2.1
-bundles=\
  io.openliberty.jakarta.mail.2.1;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.mail:jakarta.mail-api:2.1.1"
kind=beta
edition=core
WLP-Activation-Type: parallel
