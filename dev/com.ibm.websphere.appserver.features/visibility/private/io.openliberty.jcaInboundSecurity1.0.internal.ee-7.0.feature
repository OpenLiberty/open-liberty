-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.jcaInboundSecurity1.0.internal.ee-7.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
visibility = private
-features=\
  com.ibm.websphere.appserver.eeCompatible-7.0; ibm.tolerates:="8.0", \
  com.ibm.websphere.appserver.jca-1.7, \
  com.ibm.websphere.appserver.transaction-1.2
kind=ga
edition=base
