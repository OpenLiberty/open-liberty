-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.ejb-3.1
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.ejb.3.1; location:="dev/api/spec/,lib/"; mavenCoordinates="org.glassfish:javax.ejb:3.1"
-features=com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
kind=ga
edition=core
WLP-Activation-Type: parallel
