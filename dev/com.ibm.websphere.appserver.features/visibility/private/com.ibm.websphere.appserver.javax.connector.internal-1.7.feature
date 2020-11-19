-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.connector.internal-1.7
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.connector.1.7; apiJar=false; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.resource:javax.resource-api:1.7"
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
