-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wimcore-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=\
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.ssl-1.0
-bundles=\
  com.ibm.ws.security.wim.core, \
  com.ibm.websphere.security.wim.base
kind=ga
edition=core
