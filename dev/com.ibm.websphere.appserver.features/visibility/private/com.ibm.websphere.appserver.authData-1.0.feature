-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.authData-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.jcaSecurity-1.0
-bundles=com.ibm.ws.security.authentication, \
 com.ibm.ws.security.jaas.common, \
 com.ibm.ws.security.auth.data
kind=ga
edition=base
