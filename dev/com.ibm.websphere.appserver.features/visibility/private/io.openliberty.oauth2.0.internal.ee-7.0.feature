-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
  com.ibm.websphere.appserver.jsp-2.3, \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0", \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0"
-bundles=\
  com.ibm.ws.security.oauth.2.0, \
  com.ibm.ws.security.jwt, \
  com.ibm.ws.security.common
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oauth_1.2-javadoc.zip
kind=ga
edition=core
