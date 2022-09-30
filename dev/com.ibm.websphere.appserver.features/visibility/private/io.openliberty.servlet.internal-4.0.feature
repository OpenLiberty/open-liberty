-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.servlet.internal-4.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: install, uninstall
-features= \
  com.ibm.websphere.appserver.servlet-4.0
kind=ga
edition=core
WLP-Activation-Type: parallel
