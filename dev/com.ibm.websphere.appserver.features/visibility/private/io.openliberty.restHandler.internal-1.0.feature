-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restHandler.internal-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
-features=com.ibm.websphere.appserver.json-1.0, \
  io.openliberty.webBundleSecurity.internal-1.0, \
  com.ibm.websphere.appserver.ssl-1.0, \
  io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.restHandler1.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0", \
  com.ibm.websphere.appserver.httptransport-1.0
-bundles=com.ibm.ws.org.joda.time.1.6.2, \
 io.openliberty.jsonsupport.internal, \
 com.ibm.websphere.rest.handler
kind=ga
edition=core
