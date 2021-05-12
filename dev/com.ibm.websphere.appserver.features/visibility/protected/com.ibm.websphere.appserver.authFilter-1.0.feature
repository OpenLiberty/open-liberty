-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.authFilter-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=protected
-features=io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0,5.0", \
  io.openliberty.authFilter1.0.internal.ee-6.0; ibm.tolerates:="9.0"
kind=ga
edition=core
