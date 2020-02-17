-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.builtinAuthorization-1.0
-features=com.ibm.websphere.appserver.securityInfrastructure-1.0
-bundles=com.ibm.ws.security.authorization, \
 com.ibm.websphere.security, \
 com.ibm.ws.security.authorization.builtin
kind=ga
edition=core
WLP-Activation-Type: parallel
