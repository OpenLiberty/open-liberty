-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.builtinAuthorization-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.securityInfrastructure-1.0
-bundles=com.ibm.ws.security.authorization, \
 com.ibm.websphere.security, \
 com.ibm.ws.security.authorization.builtin
kind=ga
edition=core
