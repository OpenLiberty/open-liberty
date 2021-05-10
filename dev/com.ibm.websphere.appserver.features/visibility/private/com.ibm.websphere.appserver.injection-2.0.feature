-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.injection-2.0
singleton=true
IBM-Process-Types: client, \
 server
-features=com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.anno-2.0
-bundles=com.ibm.ws.injection.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
