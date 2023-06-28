-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.ee-11.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-6.0, \
  com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.samlWeb2.0.internal.ee9.opensaml-3.4
-bundles=\
  io.openliberty.security.saml.internal.wab.2.0, \
  io.openliberty.security.common.internal
kind=noship
edition=full
