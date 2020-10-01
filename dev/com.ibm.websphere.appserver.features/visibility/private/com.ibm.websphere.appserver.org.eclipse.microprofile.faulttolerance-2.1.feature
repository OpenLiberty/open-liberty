-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-2.1
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.javax.cdi-2.0
-bundles=com.ibm.websphere.org.eclipse.microprofile.faulttolerance.2.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:2.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
