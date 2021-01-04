-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.jwt-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=io.openliberty.mpCompatible-0.0
-bundles=com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0"
kind=ga
edition=core
