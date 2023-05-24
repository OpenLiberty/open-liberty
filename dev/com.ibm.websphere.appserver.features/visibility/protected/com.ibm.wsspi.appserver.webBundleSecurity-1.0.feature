-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.wsspi.appserver.webBundleSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
-features=io.openliberty.webBundleSecurity.internal-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0", \
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0"
kind=ga
edition=core
