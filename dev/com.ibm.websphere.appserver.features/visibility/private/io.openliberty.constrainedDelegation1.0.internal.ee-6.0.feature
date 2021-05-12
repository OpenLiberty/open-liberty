-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.constrainedDelegation1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0", \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:="3.0"
kind=ga
edition=core
