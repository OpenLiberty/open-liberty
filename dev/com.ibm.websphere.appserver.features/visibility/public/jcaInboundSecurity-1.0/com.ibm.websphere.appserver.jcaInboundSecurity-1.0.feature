-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaInboundSecurity-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: javax.security.auth.message.callback; type="spec", \
  com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
IBM-ShortName: jcaInboundSecurity-1.0
Subsystem-Name: Java Connector Architecture Security Inflow 1.0
-features=\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0", \
  io.openliberty.jcaInboundSecurity1.0.internal.ee-6.0; ibm.tolerates:=7.0, \
  com.ibm.websphere.appserver.security-1.0, \
  io.openliberty.securityAPI.javaee-1.0, \
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2"
-bundles=\
   com.ibm.ws.jca.inbound.security, \
   com.ibm.websphere.javaee.jaspic.1.1; location:=dev/api/spec/; mavenCoordinates="javax.security.auth.message:javax.security.auth.message-api:1.1"
kind=ga
edition=base
WLP-Platform: javaee-6.0,javaee-7.0,javaee-8.0
WLP-InstantOn-Enabled: true; type:=beta
