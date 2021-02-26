-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.ejb-3.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.ejb.3.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.ejb:javax.ejb-api:3.2"
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="6.0, 8.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
