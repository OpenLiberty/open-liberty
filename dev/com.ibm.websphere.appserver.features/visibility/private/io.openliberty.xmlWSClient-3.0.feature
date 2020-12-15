-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWSClient-3.0
Subsystem-Name: Internal JAX-WS Client Container Features
singleton=true
WLP-DisableAllFeatures-OnConflict: false
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
-features=\
  io.openliberty.xmlws.common-3.0
-bundles=\
  com.ibm.ws.jaxws.2.3.clientcontainer.jakarta
kind=noship
edition=full