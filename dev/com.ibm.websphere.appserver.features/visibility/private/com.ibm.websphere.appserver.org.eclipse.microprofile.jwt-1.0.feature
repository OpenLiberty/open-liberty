-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.jwt-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/";type=jar; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0"
kind=ga
edition=core
