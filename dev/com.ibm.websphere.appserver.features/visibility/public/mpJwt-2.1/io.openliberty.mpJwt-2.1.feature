-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwt-2.1
visibility=public
singleton=true
IBM-ShortName: mpJwt-2.1
IBM-API-Package: \
  org.eclipse.microprofile.jwt; type="stable", \
  org.eclipse.microprofile.auth; type="stable"
Subsystem-Name: MicroProfile JSON Web Token 2.1
-features=io.openliberty.servlet.internal-6.0, \
  com.ibm.websphere.appserver.jwt-1.0, \
  io.openliberty.jsonp-2.1, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.org.eclipse.microprofile.jwt-2.1, \
  io.openliberty.mpCompatible-6.0, \
  io.openliberty.cdi-4.0
-bundles=io.openliberty.security.mp.jwt.internal,\
  io.openliberty.security.mp.jwt.cdi.internal,\
  io.openliberty.security.mp.jwt.2.1.config, \
  com.ibm.ws.webcontainer.security.app, \
  com.ibm.ws.security.appbnd
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
