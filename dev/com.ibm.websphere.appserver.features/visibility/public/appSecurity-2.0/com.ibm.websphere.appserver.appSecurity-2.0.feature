-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurity-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: \
  com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-ShortName: appSecurity-2.0
Subsystem-Name: Application Security 2.0
-features=com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0", \
  com.ibm.websphere.appserver.security-1.0, \
  io.openliberty.securityAPI.javaee-1.0
kind=ga
edition=core
WLP-InstantOn-Enabled: true
