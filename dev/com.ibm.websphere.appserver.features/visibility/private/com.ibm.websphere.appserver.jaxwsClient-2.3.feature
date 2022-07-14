-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsClient-2.3
Subsystem-Name: Internal JAX-WS Client Container Features
singleton=true
WLP-Activation-Type: parallel
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
-features=\
  com.ibm.websphere.appserver.jaxws.common-2.3
-bundles=\
  com.ibm.ws.jaxws.2.3.clientcontainer
kind=noship
edition=full