-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.api-3.1
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
-bundles=com.ibm.websphere.javaee.servlet.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet:javax.servlet-api:3.1.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
