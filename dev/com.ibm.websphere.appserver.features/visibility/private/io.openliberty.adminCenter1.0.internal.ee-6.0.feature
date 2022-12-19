-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.adminCenter1.0.internal.ee-6.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.javax.jsp-2.2; ibm.tolerates:="2.3"
-bundles=\
  com.ibm.ws.ui.servlet.filter
kind=ga
edition=core
