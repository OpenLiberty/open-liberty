-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWSClient-4.0
Subsystem-Name: Internal JAX-WS Client Container Features
WLP-Activation-Type: parallel
singleton=true
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
-features=\
  io.openliberty.xmlws.common-4.0
-bundles=\
  com.ibm.ws.jaxws.clientcontainer.jakarta
kind=noship
edition=full