-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwtSso1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.appSecurity-4.0, \
  io.openliberty.jsonp-2.0, \
  io.openliberty.org.eclipse.microprofile.jwt-2.0
-bundles=\
  io.openliberty.security.common.internal, \
  io.openliberty.security.jwtsso.internal, \
  io.openliberty.security.mp.jwt.internal
kind=ga
edition=core
