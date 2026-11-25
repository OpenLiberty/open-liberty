-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.safAuthorization-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: com.ibm.wsspi.security.tai;  type="ibm-api", \
 com.ibm.wsspi.security.token;  type="ibm-api", \
 com.ibm.wsspi.security.auth.callback;  type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module;  type="ibm-api", \
 com.ibm.wsspi.security.authorization.saf;  type="ibm-api", \
 com.ibm.wsspi.security.credentials.saf;  type="ibm-api"
Manifest-Version: 1.0
-features=com.ibm.websphere.appserver.builtinAuthentication-1.0
-bundles=com.ibm.ws.security.credentials, \
 com.ibm.ws.security.authorization.saf, \
 com.ibm.ws.security.authorization, \
 com.ibm.ws.security.credentials.saf
-jars=com.ibm.websphere.appserver.api.security.authorization.saf; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security.authorization.saf_1.3-javadoc.zip
kind=ga
edition=zos
