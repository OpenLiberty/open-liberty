-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwtSso-1.0
visibility=public
singleton=true
IBM-ShortName: jwtSso-1.0
# we don't need servlet 4.0, but we specify it to suppress
# a CWWKF0001E from jwt's use of servlet 3.0
Subsystem-Name: JSON Web Token Single Sign-On 1.0
 -features=com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.appSecurity-2.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:=4.0, \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:=1.1, \
  com.ibm.websphere.appserver.httpcommons-1.0
-bundles= com.ibm.ws.security.jwtsso, \
  com.ibm.ws.security.common, \
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/",\
  com.ibm.ws.security.mp.jwt,\
  com.ibm.ws.org.apache.commons.codec.1.4, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=ga
edition=core
