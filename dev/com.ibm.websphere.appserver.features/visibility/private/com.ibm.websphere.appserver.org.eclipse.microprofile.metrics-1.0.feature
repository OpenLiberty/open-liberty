-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.metrics-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.org.eclipse.microprofile.metrics.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.metrics:microprofile-metrics-api:1.0"
kind=ga
edition=core
