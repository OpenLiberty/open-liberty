-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.ee9.opensaml-3.4
visibility=private
singleton=true
-bundles=com.ibm.ws.org.apache.santuario.xmlsec.2.2.0.jakarta, \
  com.ibm.ws.com.google.guava, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.cryptacular.cryptacular.1.2.4, \
  com.ibm.ws.org.ehcache.ehcache.107.3.8.1.jakarta, \
  com.ibm.ws.org.jasypt.jasypt.1.9.3, \
  com.ibm.ws.org.joda.time.2.9.9, \
  com.ibm.ws.net.shibboleth.utilities.java.support.7.5.1.jakarta, \
  com.ibm.ws.org.opensaml.opensaml.core.3.4.5.jakarta, \
  com.ibm.ws.org.opensaml.opensaml.messaging.api.3.4.5.jakarta, \
  com.ibm.ws.org.opensaml.opensaml.messaging.impl.3.4.5.jakarta, \
  com.ibm.ws.org.opensaml.opensaml.storage.api.3.4.5.jakarta, \
  com.ibm.ws.security.saml.websso.2.0.jakarta
kind=ga
edition=core
