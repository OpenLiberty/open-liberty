-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.webCache1.0.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
  com.ibm.websphere.appserver.jsp-2.3, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0"
-bundles=\
  com.ibm.ws.dynacache.web
kind=ga
edition=core
