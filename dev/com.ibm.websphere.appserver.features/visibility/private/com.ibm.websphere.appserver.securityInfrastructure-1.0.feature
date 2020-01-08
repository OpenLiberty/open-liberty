-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.securityInfrastructure-1.0
-features=com.ibm.websphere.appserver.classloading-1.0
-bundles=com.ibm.ws.security.authentication, \
 com.ibm.ws.security.credentials, \
 com.ibm.ws.security.token, \
 com.ibm.ws.security.authorization, \
 com.ibm.ws.security, \
 com.ibm.websphere.security, \
 com.ibm.ws.security.registry; start-phase:=CONTAINER_EARLY, \
 com.ibm.ws.management.security, \
 com.ibm.ws.security.ready.service, \
 com.ibm.ws.security.mp.jwt.proxy
kind=ga
edition=core
WLP-Activation-Type: parallel
