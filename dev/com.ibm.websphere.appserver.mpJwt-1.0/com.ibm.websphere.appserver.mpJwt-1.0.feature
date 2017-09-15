-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpJwt-1.0
visibility=public
IBM-ShortName: mpJwt-1.0
IBM-API-Package: org.eclipse.microprofile.jwt; type="stable", \
                 org.eclipse.microprofile.auth; type="stable"
Subsystem-Name: Micro Profile Json Web Token 1.0
-features=com.ibm.websphere.appserver.servlet-3.1, \
  com.ibm.websphere.appserver.cdi-1.2, \
  com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.jsonp-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0
-bundles=com.ibm.ws.security.mp.jwt,\
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/",\
  com.ibm.ws.security.mp.jwt.cdi,\
  com.ibm.ws.org.apache.commons.codec.1.4, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=ga
edition=core
Require-Capability: osgi.ee;filter:="(&(osgi.ee=JavaSE)(version=1.8))"
