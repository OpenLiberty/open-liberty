-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-10.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.api-6.0; ibm.tolerates:="6.1"; apiJar=false
-bundles=\
  io.openliberty.webservices.handler
kind=ga
edition=core
WLP-Activation-Type: parallel