-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.metrics-5.1
singleton=true
-features=io.openliberty.mpCompatible-6.1; ibm.tolerates:="7.0"
-bundles=io.openliberty.org.eclipse.microprofile.metrics.5.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.metrics:microprofile-metrics-api:5.1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel