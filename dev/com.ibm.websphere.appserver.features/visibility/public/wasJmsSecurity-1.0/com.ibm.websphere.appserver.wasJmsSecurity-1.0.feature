-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wasJmsSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: \
  com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-ShortName: wasJmsSecurity-1.0
Subsystem-Name: Message Server Security 1.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0, 8.0", \
  com.ibm.websphere.appserver.wasJmsServer-1.0, \
  com.ibm.websphere.appserver.security-1.0, \
  io.openliberty.securityAPI.javaee-1.0, \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2"
-bundles=\
  com.ibm.ws.messaging.utils, \
  com.ibm.ws.messaging.security, \
  com.ibm.ws.messaging.security.common
kind=ga
edition=base
