-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.rest.client-3.0
singleton=true
# io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1" comes from io.openliberty.microprofile.cdi.api features
-features=\
  io.openliberty.jakarta.annotation-2.0; ibm.tolerates:="2.1", \
  io.openliberty.microprofile.cdi.api-3.0; ibm.tolerates:="4.0", \
  io.openliberty.jakarta.restfulWS-3.0; ibm.tolerates:="3.1", \
  io.openliberty.org.eclipse.microprofile.config-3.0; ibm.tolerates:="3.1"
-bundles=io.openliberty.org.eclipse.microprofile.rest.client.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.rest.client:microprofile-rest-client-api:3.0.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
