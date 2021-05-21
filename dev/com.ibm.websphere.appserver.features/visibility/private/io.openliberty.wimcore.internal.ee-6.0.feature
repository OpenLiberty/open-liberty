-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.wimcore.internal.ee-6.0
Subsystem-Version: 6.0
singleton=true
visibility = private
WLP-DisableAllFeatures-OnConflict: false
-features=\
  com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0"
-bundles=\
  com.ibm.websphere.security.wim.base
kind=ga
edition=core
