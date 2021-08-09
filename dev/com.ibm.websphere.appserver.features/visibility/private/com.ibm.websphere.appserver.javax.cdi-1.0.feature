-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.cdi-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
-bundles=com.ibm.websphere.javaee.cdi.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise:cdi-api:1.0-SP4"
kind=ga
edition=core
