-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.api-3.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
-bundles=com.ibm.websphere.javaee.servlet.3.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet:javax.servlet-api:3.0.1"
kind=ga
edition=core
WLP-Activation-Type: parallel
