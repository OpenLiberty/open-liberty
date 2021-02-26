-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=\
 com.ibm.ws.security.auth.data.common, \
 com.ibm.ws.security.authentication, \
 com.ibm.ws.security.credentials, \
 com.ibm.ws.security.kerberos.auth,\
 com.ibm.websphere.security
kind=ga
edition=core
WLP-Activation-Type: parallel
