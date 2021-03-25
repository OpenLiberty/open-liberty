-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.authFilter1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.api-5.0
-bundles=\
  io.openliberty.security.authentication.internal.filter
kind=beta
edition=core
WLP-Activation-Type: parallel
