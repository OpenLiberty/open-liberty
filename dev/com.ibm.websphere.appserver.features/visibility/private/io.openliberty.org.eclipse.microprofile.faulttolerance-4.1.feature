-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.org.eclipse.microprofile.faulttolerance-4.1
singleton=true
-features=\
 io.openliberty.mpCompatible-7.0,\
 io.openliberty.microprofile.cdi.api-4.0; ibm.tolerates:="4.1"
# TODO before GA, check maven coordinates 
-bundles=io.openliberty.org.eclipse.microprofile.faulttolerance.4.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:4.1.0"
kind=beta
edition=core
WLP-Activation-Type: parallel
