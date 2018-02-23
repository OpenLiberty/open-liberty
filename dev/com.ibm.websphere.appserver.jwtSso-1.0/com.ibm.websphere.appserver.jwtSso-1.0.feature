-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwtSso-1.0
visibility=public
singleton=true
IBM-ShortName: jwtSso-1.0
# we don't need servlet 4.0, but we specify it to suppress
# a CWWKF0001E from jwt's use of servlet 3.0
Subsystem-Name: JwtSso
 -features=com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.appSecurity-3.0, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:=1.1
-bundles= com.ibm.ws.security.jwtsso, \
  com.ibm.ws.security.common, \
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/",\
  com.ibm.ws.security.mp.jwt
kind=noship
edition=full
