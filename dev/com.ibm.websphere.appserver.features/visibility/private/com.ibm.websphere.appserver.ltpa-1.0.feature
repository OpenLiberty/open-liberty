-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ltpa-1.0
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.ws.security.credentials, \
 com.ibm.ws.security.token, \
 com.ibm.websphere.security, \
 com.ibm.ws.security.credentials.ssotoken, \
 com.ibm.ws.security.token.ltpa, \
 com.ibm.ws.crypto.ltpakeyutil
kind=ga
edition=core
WLP-Activation-Type: parallel
