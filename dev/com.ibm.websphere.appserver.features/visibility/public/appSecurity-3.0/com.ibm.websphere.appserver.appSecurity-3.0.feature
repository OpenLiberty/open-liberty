-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.appSecurity-3.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: javax.security.enterprise; type="spec", \
 javax.security.enterprise.authentication.mechanism.http; type="spec", \
 javax.security.enterprise.credential; type="spec", \
 javax.security.enterprise.identitystore; type="spec", \
 javax.security.auth.message; type="spec", \
 javax.security.auth.message.callback; type="spec", \
 javax.security.auth.message.config; type="spec", \
 javax.security.auth.message.module; type="spec", \
 com.ibm.wsspi.security.tai; type="ibm-api", \
 com.ibm.wsspi.security.token; type="ibm-api", \
 com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api"

IBM-ShortName: appSecurity-3.0
Subsystem-Name: Application Security 3.0
-features=com.ibm.websphere.appserver.servlet-4.0, \
  io.openliberty.servlet.internal-4.0, \
  com.ibm.websphere.appserver.eeCompatible-8.0, \
  com.ibm.websphere.appserver.el-3.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.security-1.0, \
  io.openliberty.securityAPI.javaee-1.0
-bundles=com.ibm.websphere.javaee.security.1.0; location:=dev/api/spec/; mavenCoordinates="javax.security.enterprise:javax.security.enterprise-api:1.0", \
 com.ibm.ws.security.javaeesec.1.0, \
 com.ibm.ws.security.javaeesec.cdi, \
 com.ibm.websphere.javaee.jaspic.1.1; location:=dev/api/spec/; mavenCoordinates="javax.security.auth.message:javax.security.auth.message-api:1.1", \
 com.ibm.ws.security.jaspic.1.1
kind=ga
edition=core
-jars=com.ibm.websphere.appserver.spi.jaspic; location:=dev/spi/ibm/
-files=dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.jaspic_1.1-javadoc.zip
WLP-InstantOn-Enabled: true
