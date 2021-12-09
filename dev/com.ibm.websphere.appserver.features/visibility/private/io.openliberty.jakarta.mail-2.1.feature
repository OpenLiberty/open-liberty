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
  io.openliberty.jakarta.mail.2.0;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.mail:jakarta.mail-api:2.0.0",\
   io.openliberty.com.sun.mail.jakarta.mail.2.0
kind=noship
edition=full
WLP-Activation-Type: parallel
