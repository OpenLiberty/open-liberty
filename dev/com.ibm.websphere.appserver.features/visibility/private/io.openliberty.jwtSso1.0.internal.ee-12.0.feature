-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwtSso1.0.internal.ee-12.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.appSecurity-7.0, \
  io.openliberty.jsonp-2.2, \
  io.openliberty.org.eclipse.microprofile.jwt-2.1
-bundles=\
  io.openliberty.security.common.internal, \
  io.openliberty.security.mp.jwt.internal
kind=noship
edition=full
