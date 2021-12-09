-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-5.0; ibm.tolerates:="6.0"
-bundles=\
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
kind=ga
edition=core
