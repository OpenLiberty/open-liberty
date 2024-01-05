-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.config-3.1
singleton=true
-features=io.openliberty.mpCompatible-6.1, \
  io.openliberty.jakarta.cdi-4.0
-bundles=io.openliberty.org.eclipse.microprofile.config.3.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:3.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
