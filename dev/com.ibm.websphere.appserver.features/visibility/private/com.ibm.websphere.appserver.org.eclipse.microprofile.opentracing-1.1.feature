-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.opentracing-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=com.ibm.websphere.org.eclipse.microprofile.opentracing.1.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.opentracing:microprofile-opentracing-api:1.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
