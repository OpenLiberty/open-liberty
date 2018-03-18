-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurityClient-1.0
visibility=public
IBM-API-Package: com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api", \
 com.ibm.websphere.security; type="ibm-api"
IBM-ShortName: appSecurityClient-1.0
Subsystem-Name: Application Security for Client 1.0
-features=com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1,4.0"; apiJar=false, \
 com.ibm.websphere.appserver.ssl-1.0, \
 com.ibm.websphere.appserver.csiv2Client-1.0
-bundles=com.ibm.ws.security.authentication, \
 com.ibm.ws.security.credentials, \
 com.ibm.ws.security.token, \
 com.ibm.ws.security.authorization, \
 com.ibm.ws.security.client, \
 com.ibm.ws.security, \
 com.ibm.ws.security.jaas.common, \
 com.ibm.ws.security.registry, \
 com.ibm.websphere.security.impl, \
 com.ibm.ws.security.mp.jwt.proxy
-jars=com.ibm.websphere.appserver.api.securityClient; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.securityClient_1.1-javadoc.zip
kind=ga
edition=base
