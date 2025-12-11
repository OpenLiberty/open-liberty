-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.samlWeb-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: samlWeb-2.0
Subsystem-Name: SAML Web Single Sign-On 2.0
-features=io.openliberty.samlWeb2.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0, 11.0", \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  com.ibm.websphere.appserver.ssoCommon-1.0, \
  io.openliberty.org.bouncycastle
-bundles=\
  io.openliberty.org.apache.commons.logging, \
  io.openliberty.org.apache.commons.codec, \
  com.ibm.ws.org.jose4j
kind=ga
edition=core
