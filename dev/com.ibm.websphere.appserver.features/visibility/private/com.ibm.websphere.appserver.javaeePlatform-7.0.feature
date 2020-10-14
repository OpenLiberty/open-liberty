-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.javaeePlatform-7.0
WLP-DisableAllFeatures-OnConflict: false
IBM-Process-Types: client, server
-features=com.ibm.websphere.appserver.javaeePlatform-6.0
-bundles=com.ibm.ws.javaee.platform.defaultresource, \
 com.ibm.ws.javaee.platform.v7, \
 com.ibm.ws.javaee.version
kind=ga
edition=core
WLP-Activation-Type: parallel
