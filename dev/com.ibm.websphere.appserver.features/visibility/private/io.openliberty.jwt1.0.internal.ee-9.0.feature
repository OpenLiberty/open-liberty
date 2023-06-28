-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.internal-5.0; ibm.tolerates:="6.0, 6.1"
-bundles=\
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal, \
  io.openliberty.security.common.jwt.internal
kind=ga
edition=core
