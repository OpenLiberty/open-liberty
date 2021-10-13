-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwt-2.0
visibility=public
singleton=true
IBM-ShortName: mpJwt-2.0
IBM-API-Package: \
  org.eclipse.microprofile.jwt; type="stable", \
  org.eclipse.microprofile.auth; type="stable"
Subsystem-Name: MicroProfile JSON Web Token 2.0
-features=com.ibm.websphere.appserver.jwt-1.0, \
  io.openliberty.appSecurity-4.0, \
  io.openliberty.jsonp-2.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  io.openliberty.mpConfig-3.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.org.eclipse.microprofile.jwt-2.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.cdi-3.0, \
  com.ibm.websphere.appserver.authFilter-1.0
-bundles=io.openliberty.security.mp.jwt.internal,\
  io.openliberty.security.mp.jwt.cdi.internal,\
  io.openliberty.security.mp.jwt.1.2.config,\
  com.ibm.ws.security.mp.jwt.1.1.config
kind=noship
edition=full
WLP-Activation-Type: parallel
