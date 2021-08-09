-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsf-2.3
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-8.0; ibm.tolerates:="6.0, 7.0"
-bundles=com.ibm.websphere.javaee.jsf.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.myfaces.core:myfaces-api:2.3.9"
kind=ga
edition=core
WLP-Activation-Type: parallel
