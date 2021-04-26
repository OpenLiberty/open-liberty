-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2, 2.0", \
 io.openliberty.jcaSecurity.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
 com.ibm.ws.security.auth.data.common, \
 com.ibm.ws.security.authentication, \
 com.ibm.ws.security.credentials, \
 com.ibm.ws.security.kerberos.auth,\
 com.ibm.websphere.security
kind=ga
edition=core
WLP-Activation-Type: parallel
