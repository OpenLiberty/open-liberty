-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.zosSecurity-1.0
visibility=public
IBM-API-Package: com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-ShortName: zosSecurity-1.0
Subsystem-Name: z/OS SAF Integration 1.0
-features=com.ibm.websphere.appserver.safRegistry-1.0, \
 com.ibm.websphere.appserver.securityInfrastructure-1.0, \
 com.ibm.websphere.appserver.safAuthorization-1.0, \
 com.ibm.websphere.appserver.builtinAuthentication-1.0
-bundles=com.ibm.websphere.security.impl
kind=ga
edition=zos
