-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.4
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:="2.0"
-bundles=com.ibm.websphere.org.eclipse.microprofile.config.1.4; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:1.4"
kind=ga
edition=core
WLP-Activation-Type: parallel
