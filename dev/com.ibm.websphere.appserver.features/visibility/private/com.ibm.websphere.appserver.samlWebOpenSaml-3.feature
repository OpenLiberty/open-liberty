-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = com.ibm.websphere.appserver.samlWebOpenSaml-3
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=\
  com.ibm.ws.org.apache.commons.logging.1.0.3, \
  com.ibm.ws.org.apache.commons.codec, \
  com.ibm.json4j, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.com.google.guava, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
  com.ibm.ws.org.joda.time.2.9.9, \
  com.ibm.ws.net.shibboleth.utilities.java.support.7.5.1, \
  com.ibm.ws.org.opensaml.opensaml.core.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.messaging.api.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.messaging.impl.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.storage.api.3.4.5, \
  com.ibm.ws.security.saml.websso.2.0,\
  com.ibm.ws.security.saml.wab.2.0, \
  com.ibm.ws.security.common
kind=noship
edition=full
