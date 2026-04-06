-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1, 4.0"
-bundles=\
  com.ibm.ws.security.jwt, \
  com.ibm.ws.security.common
kind=ga
edition=core
