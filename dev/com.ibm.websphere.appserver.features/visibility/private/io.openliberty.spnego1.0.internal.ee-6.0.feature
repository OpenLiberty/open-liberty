-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.spnego1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0", \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"
-bundles=\
  com.ibm.ws.security.spnego
kind=ga
edition=core
