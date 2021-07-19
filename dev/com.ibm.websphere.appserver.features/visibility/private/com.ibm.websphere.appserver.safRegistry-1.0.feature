-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.safRegistry-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: com.ibm.wsspi.security.registry.saf;  type="ibm-api"
-features=com.ibm.websphere.appserver.securityInfrastructure-1.0
-bundles=com.ibm.ws.security.registry.saf, \
 com.ibm.ws.security.credentials, \
 com.ibm.ws.security.registry, \
 com.ibm.ws.security.credentials.saf; start-phase:=CONTAINER_EARLY
-jars=com.ibm.websphere.appserver.api.security.registry.saf; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security.registry.saf_1.0-javadoc.zip
kind=ga
edition=zos
