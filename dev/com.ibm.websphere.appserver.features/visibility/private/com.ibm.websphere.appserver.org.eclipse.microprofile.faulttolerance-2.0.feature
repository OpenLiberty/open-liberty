-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.faulttolerance-2.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.javax.cdi-2.0
-bundles=com.ibm.websphere.org.eclipse.microprofile.faulttolerance.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:2.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
