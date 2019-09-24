-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaSecurity-1.0
-features=com.ibm.websphere.appserver.containerServices-1.0
-bundles=com.ibm.ws.security.jca, \
 com.ibm.ws.security.auth.data.common, \
 com.ibm.ws.security.credentials, \
 com.ibm.websphere.security
kind=ga
edition=core
WLP-Activation-Type: parallel
