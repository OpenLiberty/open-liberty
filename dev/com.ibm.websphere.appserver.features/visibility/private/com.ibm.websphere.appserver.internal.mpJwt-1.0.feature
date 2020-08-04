-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.mpJwt-1.0
singleton=true
IBM-API-Package: \
  org.eclipse.microprofile.jwt; type="stable", \
  org.eclipse.microprofile.auth; type="stable"
Subsystem-Name: MicroProfile JSON Web Token internal 1.0
-features=com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:=4.0, \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:=2.0, \
  com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.jsonp-1.0; ibm.tolerates:=1.1, \
  com.ibm.websphere.appserver.httpcommons-1.0
-bundles=com.ibm.ws.security.mp.jwt,\
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0",\
  com.ibm.ws.security.mp.jwt.cdi,\
  com.ibm.ws.org.apache.commons.codec, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=ga
edition=core
