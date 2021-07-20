-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.javaee.websocket-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
-bundles=com.ibm.websphere.javaee.websocket.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.websocket:javax.websocket-api:1.0"
kind=ga
edition=core
