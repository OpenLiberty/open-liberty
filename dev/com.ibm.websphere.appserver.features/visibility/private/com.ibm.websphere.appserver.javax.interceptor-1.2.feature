-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.interceptor-1.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.interceptor.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.interceptor:javax.interceptor-api:1.2"
kind=ga
edition=core
WLP-Activation-Type: parallel
