-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wsAtomicTransaction1.2.internal.ee-6.0
singleton=true
visibility = private
WLP-DisableAllFeatures-OnConflict: false
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  io.openliberty.wsAtomicTransaction1.2.internal.jaxws-2.2; ibm.tolerates:="2.3"
kind=ga
edition=base
