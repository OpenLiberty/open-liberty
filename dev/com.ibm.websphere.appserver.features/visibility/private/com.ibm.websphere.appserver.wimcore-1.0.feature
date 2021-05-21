-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wimcore-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=\
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.ssl-1.0, \
  io.openliberty.wimcore.internal.ee-6.0; ibm.tolerates:=9.0, \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0,9.0"
-bundles=\
  com.ibm.ws.security.wim.core
kind=ga
edition=core
