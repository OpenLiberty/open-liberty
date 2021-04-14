-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.socialLogin1.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.socialLogin-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
IBM-Install-Policy: when-satisfied
-features=\
  io.openliberty.jsonp-2.0
-bundles=\
  io.openliberty.security.social,\
  io.openliberty.security.openidconnect.internal.clients.common
kind=noship
edition=full
