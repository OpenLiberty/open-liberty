-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.distributedMapInternal-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
Subsystem-Version: 1.0.0
-features=io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0"
-bundles=\
  com.ibm.ws.dynacache
kind=ga
edition=core
WLP-Activation-Type: parallel
