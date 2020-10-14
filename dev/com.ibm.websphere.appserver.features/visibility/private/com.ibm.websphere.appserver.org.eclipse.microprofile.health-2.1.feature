-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.health-2.1
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.org.eclipse.microprofile.health.2.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.health:microprofile-health-api:2.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
