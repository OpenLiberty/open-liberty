-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.faulttolerance-4.0
singleton=true
# io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1" comes from io.openliberty.microprofile.cdi.api features
-features=io.openliberty.microprofile.cdi.api-3.0; ibm.tolerates:="4.0"
-bundles=io.openliberty.org.eclipse.microprofile.faulttolerance.4.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:4.0.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
