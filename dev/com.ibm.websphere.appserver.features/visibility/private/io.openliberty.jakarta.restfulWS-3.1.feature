-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.restfulWS-3.1
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Jakarta RESTful Web Services 3.1
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.xmlBinding-4.0; apiJar=false
-bundles=\
  io.openliberty.jakarta.restfulWS.3.1;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ws.rs:jakarta.ws.rs-api:3.1.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
