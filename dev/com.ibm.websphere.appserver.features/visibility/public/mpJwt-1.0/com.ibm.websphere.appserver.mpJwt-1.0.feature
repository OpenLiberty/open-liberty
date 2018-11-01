-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpJwt-1.0
visibility=public
singleton=true
IBM-ShortName: mpJwt-1.0
IBM-API-Package: \
  org.eclipse.microprofile.jwt; type="stable", \
  org.eclipse.microprofile.auth; type="stable"
Subsystem-Name: MicroProfile JSON Web Token 1.0
-features=com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:=4.0, \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:=2.0, \
  com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:=1.1, \
  com.ibm.websphere.appserver.httpcommons-1.0
-bundles=com.ibm.ws.security.mp.jwt,\
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0",\
  com.ibm.ws.security.mp.jwt.cdi
kind=ga
edition=core