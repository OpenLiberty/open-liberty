-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.samlWeb2.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.samlWeb-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxws-2.2)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.appSecurity-4.0, \
  com.ibm.websphere.appserver.wss4j-1.0
-bundles=\
  com.ibm.ws.org.opensaml.xmltooling.1.4.4, \
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.ws.org.apache.commons.httpclient, \
  io.openliberty.org.opensaml.opensaml.2.6.1, \
  io.openliberty.org.opensaml.openws.1.5.6, \
  io.openliberty.security.saml.internal.sso.2.0, \
  io.openliberty.security.saml.internal.wab.2.0, \
  io.openliberty.security.common.internal
kind=noship
edition=full
