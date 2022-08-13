-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.mpConfig3.0.internal.ee-10.0
singleton=true
visibility = private
-features=\
  io.openliberty.jakarta.annotation-2.1, \
  io.openliberty.mpCompatible-6.0, \
  io.openliberty.jakarta.cdi-4.0
-bundles=io.openliberty.org.eclipse.microprofile.config.3.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:3.0.1"
kind=noship
edition=full
WLP-Activation-Type: parallel
