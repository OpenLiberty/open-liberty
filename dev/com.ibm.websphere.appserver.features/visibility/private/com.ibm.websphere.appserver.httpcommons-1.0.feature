-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.httpcommons-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
-bundles=com.ibm.ws.org.apache.httpcomponents, \
 io.openliberty.org.apache.commons.codec, \
 io.openliberty.org.apache.commons.logging
kind=ga
edition=core
WLP-Activation-Type: parallel
