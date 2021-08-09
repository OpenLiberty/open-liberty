-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.messaging-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.org.eclipse.microprofile.reactive.messaging.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.reactive.messaging:microprofile-reactive-messaging-api:1.0"
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0, \
  com.ibm.websphere.appserver.javax.cdi-2.0
kind=ga
edition=core
WLP-Activation-Type: parallel
