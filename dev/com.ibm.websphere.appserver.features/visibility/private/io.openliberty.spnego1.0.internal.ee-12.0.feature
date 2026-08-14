-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.spnego1.0.internal.ee-12.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-7.0, \
  com.ibm.websphere.appserver.servlet-6.2
-bundles=\
  io.openliberty.security.spnego.internal
kind=noship
edition=full
