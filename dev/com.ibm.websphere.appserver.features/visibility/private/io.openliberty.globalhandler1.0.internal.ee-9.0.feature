-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.servlet.api-5.0; apiJar=false
-bundles=\
  io.openliberty.webservices.handler
kind=beta
edition=core
WLP-Activation-Type: parallel