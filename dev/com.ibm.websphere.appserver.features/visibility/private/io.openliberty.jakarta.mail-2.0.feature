-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.mail-2.0
visibility=private
singleton=true
Subsystem-Version: 2.0
IBM-Process-Types: client, \
 server
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.activation-2.0
-bundles=\
  io.openliberty.jakarta.mail.2.0;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.mail:jakarta.mail-api:2.0.0",\
   io.openliberty.com.sun.mail.jakarta.mail.2.0
kind=beta
edition=core
WLP-Activation-Type: parallel
