-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.spnego1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.appSecurity-4.0
-bundles=\
  io.openliberty.security.spnego.internal
kind=beta
edition=core
