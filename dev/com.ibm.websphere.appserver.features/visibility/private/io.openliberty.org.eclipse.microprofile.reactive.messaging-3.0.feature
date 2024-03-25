-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.reactive.messaging-3.0
WLP-DisableAllFeatures-OnConflict: true
singleton=true
-bundles=io.openliberty.io.smallrye.reactive.messaging-api4
-jars=io.openliberty.org.eclipse.microprofile.reactive.messaging.3.0; location:="dev/api/stable/"; mavenCoordinates="org.eclipse.microprofile.reactive.messaging:microprofile-reactive-messaging-api:3.0"
-features=io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1", \
  io.openliberty.org.eclipse.microprofile.reactive.streams.operators-3.0, \
  io.openliberty.org.eclipse.microprofile.config-3.0; ibm.tolerates:="3.1", \
  io.openliberty.jakarta.cdi-3.0; ibm.tolerates:="4.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
