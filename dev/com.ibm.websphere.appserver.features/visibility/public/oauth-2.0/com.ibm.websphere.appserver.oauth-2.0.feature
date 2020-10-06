-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.oauth-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.wsspi.security.oauth20.token;  type="ibm-api", \
 com.ibm.oauth.core.api.oauth20.mediator;  type="ibm-api", \
 com.ibm.oauth.core.api.attributes;  type="ibm-api", \
 com.ibm.oauth.core.api.error;  type="ibm-api", \
 com.ibm.oauth.core.api.error.oauth20;  type="ibm-api", \
 com.ibm.oauth.core.api.config;  type="ibm-api", \
 com.ibm.websphere.security.oauth20; type="ibm-api", \
 com.ibm.websphere.security.oauth20.store; type="ibm-api", \
 com.ibm.websphere.security.openidconnect.token; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.security.oauth20, com.ibm.wsspi.security.openidconnect
IBM-ShortName: oauth-2.0
Subsystem-Name: OAuth 2.0
-features=com.ibm.websphere.appserver.ldapRegistry-3.0, \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.jsp-2.2; ibm.tolerates:=2.3, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.json-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=com.ibm.ws.security.oauth.2.0, \
  com.ibm.ws.com.google.gson.2.2.4, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.org.apache.commons.codec, \
  com.ibm.ws.security.common, \
  com.ibm.ws.security.common.jsonwebkey, \
  com.ibm.ws.org.json.simple.1.1.1, \
  com.ibm.ws.security.jwt, \
  com.ibm.websphere.appserver.api.oauth; location:=dev/api/ibm/, \
  com.ibm.websphere.appserver.spi.oauth; location:=dev/spi/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oauth_1.2-javadoc.zip, \
 dev/spi/ibm/javadoc/com.ibm.websphere.appserver.spi.oauth_1.4-javadoc.zip
 
 
kind=ga
edition=core
