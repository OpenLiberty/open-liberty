-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.injection-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
IBM-Process-Types: client, \
 server
-features=com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.anno-1.0
-bundles=com.ibm.ws.injection
kind=ga
edition=core
WLP-Activation-Type: parallel
