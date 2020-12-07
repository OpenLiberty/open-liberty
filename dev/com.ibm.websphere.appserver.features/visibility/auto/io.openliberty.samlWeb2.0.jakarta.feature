-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.samlWeb-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.appSecurity-4.0
-bundles=\
  io.openliberty.org.opensaml.opensaml.2.6.1, \
  io.openliberty.org.opensaml.openws.1.5.6, \
  io.openliberty.security.saml.internal.sso.2.0, \
  io.openliberty.security.saml.internal.wab.2.0, \
  io.openliberty.security.common.internal
kind=noship
edition=full
