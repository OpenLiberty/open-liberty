-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectClient1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  com.ibm.websphere.appserver.javax.cdi-1.0; ibm.tolerates:="1.2,2.0"; apiJar=false, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
-bundles=\
  com.ibm.ws.security.openidconnect.client, \
  com.ibm.ws.security.openidconnect.clients.common
kind=ga
edition=core
