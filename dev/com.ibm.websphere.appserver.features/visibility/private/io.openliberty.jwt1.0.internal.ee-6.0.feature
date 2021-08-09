-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jwt1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0"
-bundles=\
  com.ibm.ws.security.jwt, \
  com.ibm.ws.security.common
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jwt_1.1-javadoc.zip
kind=ga
edition=core
