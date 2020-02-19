-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.samlWeb-2.0
visibility=public
IBM-ShortName: samlWeb-2.0
Subsystem-Name: SAML Web Single Sign-On 2.0
-features=com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.wss4j-1.0, \
  com.ibm.websphere.appserver.ssoCommon-1.0, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0
-bundles=com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.security.saml.sso.2.0, \
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.ws.security.saml.wab.2.0, \
  com.ibm.ws.org.opensaml.opensaml.2.6.1, \
  com.ibm.ws.org.opensaml.openws.1.5.6, \
  com.ibm.ws.org.apache.commons.logging.1.0.3, \
  com.ibm.ws.security.common, \
  com.ibm.json4j, \
  com.ibm.ws.org.apache.commons.codec.1.4, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.org.apache.commons.httpclient
kind=ga
edition=core
