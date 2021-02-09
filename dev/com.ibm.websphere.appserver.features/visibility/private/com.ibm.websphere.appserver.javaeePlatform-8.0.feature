-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeePlatform-8.0
WLP-DisableAllFeatures-OnConflict: false
IBM-Process-Types: client, server
-features=com.ibm.websphere.appserver.javaeePlatform-7.0
-bundles=com.ibm.ws.javaee.platform.v8, \
 com.ibm.ws.javaee.version
kind=ga
edition=core
WLP-Activation-Type: parallel
