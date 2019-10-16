-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.security-1.0
IBM-API-Package: com.ibm.wsspi.security.tai; type="ibm-api", \
 com.ibm.wsspi.security.token; type="ibm-api", \
 com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api"
-features=com.ibm.websphere.appserver.ssl-1.0, \
 com.ibm.websphere.appserver.securityInfrastructure-1.0, \
 com.ibm.websphere.appserver.builtinAuthorization-1.0, \
 com.ibm.websphere.appserver.ltpa-1.0, \
 com.ibm.websphere.appserver.builtinAuthentication-1.0, \
 com.ibm.websphere.appserver.basicRegistry-1.0
-bundles=com.ibm.websphere.security.impl, \
 com.ibm.ws.management.security, \
 com.ibm.ws.security.quickstart
-jars=com.ibm.websphere.appserver.api.security; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security_1.2-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
