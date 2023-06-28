-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.internal.ee-11.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-6.0, \
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.pages-4.0
-bundles=\
  io.openliberty.security.oauth.internal.2.0, \
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal, \
  io.openliberty.security.common.jwt.internal
kind=noship
edition=full
