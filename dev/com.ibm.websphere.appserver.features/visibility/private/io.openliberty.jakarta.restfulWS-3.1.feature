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
  io.openliberty.jakarta.activation-2.1, \
  io.openliberty.jakarta.xmlBinding-4.0; apiJar=false
-bundles=\
  io.openliberty.jaxrs30; location:="dev/api/ibm/,lib/", \
  io.openliberty.jakarta.restfulWS.3.1;location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.ws.rs:jakarta.ws.rs-api:3.1.0"
-files=\
  dev/api/ibm/javadoc/io.openliberty.jaxrs30_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
