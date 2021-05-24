-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.ee-9.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-4.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.samlWeb2.0.internal.ee9.opensaml-2.6
-bundles=\
  io.openliberty.org.opensaml.opensaml.2.6.1, \
  io.openliberty.org.opensaml.openws.1.5.6, \
  io.openliberty.security.saml.internal.sso.2.0, \
  io.openliberty.security.saml.internal.wab.2.0, \
  io.openliberty.security.common.internal
kind=beta
edition=core
