-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.opensaml-3.4
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
-bundles=com.ibm.ws.org.apache.santuario.xmlsec.2.2.0, \
  com.ibm.ws.com.google.guava, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
  com.ibm.ws.org.joda.time.2.9.9, \
  io.openliberty.net.shibboleth.utilities.java.support, \
  io.openliberty.org.opensaml.opensaml.core, \
  io.openliberty.org.opensaml.opensaml.messaging.api, \
  io.openliberty.org.opensaml.opensaml.messaging.impl, \
  io.openliberty.org.opensaml.opensaml.storage.api, \
  com.ibm.ws.security.saml.websso.2.0
kind=ga
edition=core
