-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.health-4.0
singleton=true
-features=io.openliberty.mpCompatible-5.0,\
  io.openliberty.jakarta.cdi-3.0
-bundles=io.openliberty.org.eclipse.microprofile.health.4.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.health:microprofile-health-api:4.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
