-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.xmlWS-4.0
singleton=true
-features=io.openliberty.jakarta.xmlBinding-4.0; apiJar=false, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
 io.openliberty.jakarta.xmlWS.4.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.ws:jakarta.xml.ws-api:4.0.0",\
 io.openliberty.jakarta.soap.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.xml.soap:jakarta.xml.soap-api:3.0.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
