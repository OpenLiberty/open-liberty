-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-10.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-6.0
-bundles=\
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
kind=noship
edition=full
