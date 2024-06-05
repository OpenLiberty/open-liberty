-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.rest.client-4.0
singleton=true
# io.openliberty.mpCompatible-x.x comes from io.openliberty.microprofile.cdi.api features
-features=\
  io.openliberty.jakarta.annotation-2.1; ibm.tolerates:="3.0", \
  io.openliberty.microprofile.cdi.api-4.0; ibm.tolerates:="4.1", \
  io.openliberty.jakarta.restfulWS-3.1; ibm.tolerates:="4.0", \
  io.openliberty.org.eclipse.microprofile.config-3.1
# TODO check maven coords before GA
-bundles=io.openliberty.org.eclipse.microprofile.rest.client.4.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.rest.client:microprofile-rest-client-api:4.0.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
