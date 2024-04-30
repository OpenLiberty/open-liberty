-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwtSso1.0.internal.ee-11.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.appSecurity-6.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.org.eclipse.microprofile.jwt-2.1, \
  io.openliberty.noShip-1.0
-bundles=\
  io.openliberty.security.common.internal, \
  io.openliberty.security.jwtsso.internal, \
  io.openliberty.security.mp.jwt.internal
kind=noship
edition=full
