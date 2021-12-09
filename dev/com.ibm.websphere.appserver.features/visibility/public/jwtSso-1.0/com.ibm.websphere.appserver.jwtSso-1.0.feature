-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwtSso-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-ShortName: jwtSso-1.0
IBM-API-Package: \
  org.eclipse.microprofile.jwt; type="stable", \
  org.eclipse.microprofile.auth; type="stable"
# we don't need servlet 4.0, but we specify it to suppress
# a CWWKF0001E from jwt's use of servlet 3.0
Subsystem-Name: JSON Web Token Single Sign-On 1.0
-features=com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0, 5.0, 6.0", \
  com.ibm.websphere.appserver.authFilter-1.0, \
  io.openliberty.jwtSso1.0.internal.ee-7.0; ibm.tolerates:="9.0, 10.0"
-bundles=com.ibm.ws.security.jwtsso.token
kind=ga
edition=core
