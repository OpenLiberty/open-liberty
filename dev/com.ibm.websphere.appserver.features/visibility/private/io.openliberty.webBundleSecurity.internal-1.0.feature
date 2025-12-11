-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webBundleSecurity.internal-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-features=io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.webBundle.internal-1.0, \
  com.ibm.websphere.appserver.distributedMap-1.0, \
  com.ibm.websphere.appserver.authFilter-1.0, \
  io.openliberty.webBundleSecurity1.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0"
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.webcontainer.security.feature, \
 com.ibm.ws.security.authorization.builtin
kind=ga
edition=core
