-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.authData-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
Subsystem-Version: 1.0.0
-features=\
 com.ibm.websphere.appserver.jcaSecurity-1.0, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2
-bundles=\
 com.ibm.ws.security.authentication, \
 com.ibm.ws.security.jaas.common, \
 com.ibm.ws.security.auth.data
kind=ga
edition=base
