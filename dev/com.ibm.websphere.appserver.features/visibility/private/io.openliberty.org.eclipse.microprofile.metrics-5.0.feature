-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.metrics-5.0
singleton=true
-features=io.openliberty.mpCompatible-6.0, \
  io.openliberty.noShip-1.0
-bundles=io.openliberty.org.eclipse.microprofile.metrics.4.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.metrics:microprofile-metrics-api:4.0"
kind=noship
edition=full
WLP-Activation-Type: parallel