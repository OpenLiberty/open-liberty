-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.dynamicBundle-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-Process-Types: server, \
 client
-bundles=com.ibm.ws.dynamic.bundle
kind=ga
edition=core
WLP-Activation-Type: parallel
