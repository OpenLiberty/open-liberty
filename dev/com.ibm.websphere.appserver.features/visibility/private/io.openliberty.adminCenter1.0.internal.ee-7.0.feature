-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.adminCenter1.0.internal.ee-7.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
  com.ibm.websphere.appserver.restConnector-1.0; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jsp-2.3, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0"
-bundles=\
  com.ibm.ws.ui, \
  com.ibm.ws.org.owasp.esapi.2.1.0
kind=ga
edition=core
