-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.socialLogin1.0.internal.ee-12.0
singleton=true
visibility = private
-features=com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.jsonp-2.2
-bundles=\
  io.openliberty.security.social.internal,\
  io.openliberty.security.openidconnect.internal.clients.common,\
  io.openliberty.security.oidcclientcore.internal.jakarta
kind=noship
edition=full
