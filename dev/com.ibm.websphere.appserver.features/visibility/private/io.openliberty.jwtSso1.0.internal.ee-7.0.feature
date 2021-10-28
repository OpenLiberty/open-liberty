-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwtSso1.0.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0", \
  com.ibm.websphere.appserver.org.eclipse.microprofile.jwt-1.0; ibm.tolerates:="1.2"
-bundles=\
  com.ibm.ws.security.common, \
  com.ibm.ws.security.jwtsso, \
  com.ibm.ws.security.mp.jwt
kind=ga
edition=core
