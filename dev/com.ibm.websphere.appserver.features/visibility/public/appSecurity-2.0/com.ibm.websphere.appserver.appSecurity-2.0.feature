-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurity-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: appSecurity-2.0
Subsystem-Name: Application Security 2.0
-features=com.ibm.websphere.appserver.security-1.0, \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0"
-bundles=com.ibm.ws.security.authentication.tai
kind=ga
edition=core
