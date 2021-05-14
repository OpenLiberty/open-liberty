-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.opensaml-3.4
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.wss4j-2.3
-bundles=\
  com.ibm.ws.com.google.guava, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
  com.ibm.ws.org.joda.time.2.9.9, \
  com.ibm.ws.net.shibboleth.utilities.java.support.7.5.1, \
  com.ibm.ws.org.opensaml.opensaml.core.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.messaging.api.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.messaging.impl.3.4.5, \
  com.ibm.ws.org.opensaml.opensaml.storage.api.3.4.5, \
  com.ibm.ws.security.saml.websso.2.0
kind=noship
edition=full
