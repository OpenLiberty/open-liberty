-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.socialLogin1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.jsonp-2.0
-bundles=\
  io.openliberty.security.social.internal,\
  io.openliberty.security.openidconnect.internal.clients.common
kind=beta
edition=core
