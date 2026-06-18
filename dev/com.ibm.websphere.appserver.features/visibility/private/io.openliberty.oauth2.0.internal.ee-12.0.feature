-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.internal.ee-12.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-7.0, \
  com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.pages-4.1
-bundles=\
  io.openliberty.security.oauth.internal.2.0, \
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
kind=noship
edition=full
