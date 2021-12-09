-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeePlatform-6.0
WLP-DisableAllFeatures-OnConflict: false
IBM-Process-Types: client, server
-features=com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.ws.javaee.version, \
 com.ibm.ws.app.manager.module, \
 com.ibm.ws.security.java2sec
kind=ga
edition=core
WLP-Activation-Type: parallel
