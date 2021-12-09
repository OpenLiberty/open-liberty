-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.globalhandler1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0"; apiJar=false
-bundles=\
  com.ibm.ws.webservices.handler
kind=ga
edition=core
WLP-Activation-Type: parallel