-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaee-9.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-9.1
WLP-AlsoKnownAs: jakartaee-9.0
Subsystem-Version: 9.1.0
Subsystem-Name: Jakarta EE Platform 9.1
-features=io.openliberty.mail-2.0, \
  com.ibm.websphere.appserver.restConnector-2.0, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.connectors-2.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.batch-2.0, \
  io.openliberty.webProfile-9.1, \
  io.openliberty.xmlBinding-3.0, \
  io.openliberty.messagingSecurity-3.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.concurrent-2.0, \
  io.openliberty.appAuthorization-2.0, \
  io.openliberty.xmlWS-3.0, \
  io.openliberty.appClientSupport-2.0, \
  io.openliberty.enterpriseBeans-4.0, \
  io.openliberty.messaging-3.0, \
  io.openliberty.messagingServer-3.0, \
  io.openliberty.connectorsInboundSecurity-2.0, \
  com.ibm.websphere.appserver.transaction-2.0
kind=beta
edition=base
