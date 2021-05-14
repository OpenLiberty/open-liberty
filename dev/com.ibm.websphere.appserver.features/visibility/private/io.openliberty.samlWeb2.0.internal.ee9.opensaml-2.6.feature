-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.internal.ee9.opensaml-2.6
visibility=private
singleton=true
-features=\
  com.ibm.websphere.appserver.wss4j-1.0
-bundles=\
  com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.ws.org.apache.commons.httpclient
kind=beta
edition=core
