-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javax.jsf-2.2
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-bundles=com.ibm.websphere.javaee.jsf.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="org.apache.myfaces.core:myfaces-api:2.2.12"
kind=ga
edition=core
WLP-Activation-Type: parallel
