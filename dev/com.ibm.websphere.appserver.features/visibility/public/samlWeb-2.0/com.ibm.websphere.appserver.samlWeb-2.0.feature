-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.samlWeb-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: samlWeb-2.0
Subsystem-Name: SAML Web Single Sign-On 2.0
-features=\
  com.ibm.websphere.appserver.wss4j-1.0, \
  com.ibm.websphere.appserver.ssoCommon-1.0, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0", \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  io.openliberty.samlWeb2.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.org.joda.time.1.6.2, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.json4j, \
  io.openliberty.org.apache.commons.codec, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.org.apache.commons.httpclient
kind=ga
edition=core
