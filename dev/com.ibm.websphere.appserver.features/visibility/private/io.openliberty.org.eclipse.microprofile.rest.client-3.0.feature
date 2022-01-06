-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.rest.client-3.0
singleton=true
-features=\
  io.openliberty.jakarta.annotation-2.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.jakarta.cdi-3.0, \
  io.openliberty.jakarta.restfulWS-3.0, \
  io.openliberty.org.eclipse.microprofile.config-3.0
-bundles=io.openliberty.org.eclipse.microprofile.rest.client.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.rest.client:microprofile-rest-client-api:3.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
