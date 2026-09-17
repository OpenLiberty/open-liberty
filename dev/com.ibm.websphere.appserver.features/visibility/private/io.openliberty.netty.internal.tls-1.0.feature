-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.netty.internal.tls-1.0
WLP-DisableAllFeatures-OnConflict: false
Subsystem-Name: Netty internal TLS implementation 1.0
singleton=true
-bundles=\
  io.openliberty.io.netty, \
  io.openliberty.io.netty.ssl, \
  io.openliberty.netty.internal, \
  io.openliberty.netty.internal.tls.impl
kind=ga
edition=core
WLP-Activation-Type: parallel
