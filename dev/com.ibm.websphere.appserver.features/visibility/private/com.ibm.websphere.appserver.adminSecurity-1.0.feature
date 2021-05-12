-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
singleton=true
-features=com.ibm.websphere.appserver.security-1.0, \
 com.ibm.websphere.appserver.authFilter-1.0, \
 com.ibm.websphere.appserver.distributedMap-1.0, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0"
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.webcontainer.security; start-phase:=SERVICE_EARLY, \
 com.ibm.ws.webcontainer.security.admin, \
 com.ibm.ws.security.authentication.tai, \
 com.ibm.ws.security.sso
kind=ga
edition=core
