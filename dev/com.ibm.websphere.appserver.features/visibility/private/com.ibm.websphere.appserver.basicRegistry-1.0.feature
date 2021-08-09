-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.basicRegistry-1.0
WLP-DisableAllFeatures-OnConflict: false
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.security.registry, \
 com.ibm.ws.security.registry.basic
kind=ga
edition=core
