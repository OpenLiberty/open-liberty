-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.opensaml-2.6
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.wss4j-1.0
-bundles=\
  com.ibm.ws.org.opensaml.opensaml.2.6.1, \
  com.ibm.ws.org.opensaml.openws.1.5.6, \
  com.ibm.ws.security.saml.sso.2.0,\
  com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.ws.org.apache.commons.httpclient
kind=ga
edition=core
