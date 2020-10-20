-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.connector.internal-1.6
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.connector.1.6; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.resource:javax.resource-api:1.7"
kind=ga
edition=core
WLP-Activation-Type: parallel
