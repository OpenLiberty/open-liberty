-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.restfulWS-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta RESTful Web Services 3.0
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.activation-2.0
-bundles=\
  com.ibm.websphere.appserver.api.jaxrs30.jakarta; location:="dev/api/ibm/,lib/", \
  io.openliberty.jakarta.restfulWS.3.0;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ws.rs:jakarta.ws.rs-api:3.0.0"
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jaxrs30_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
