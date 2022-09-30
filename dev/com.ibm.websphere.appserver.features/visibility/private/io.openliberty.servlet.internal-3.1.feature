-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.internal-3.1
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: install, uninstall
-features= \
  com.ibm.websphere.appserver.servlet-3.1
kind=ga
edition=core
WLP-Activation-Type: parallel
