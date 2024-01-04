-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaee-10.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-10.0
Subsystem-Version: 10.0.0
Subsystem-Name: Jakarta EE Platform 10.0
-features=io.openliberty.mail-2.1, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.batch-2.1, \
  io.openliberty.webProfile-10.0, \
  io.openliberty.xmlBinding-4.0, \
  io.openliberty.messagingSecurity-3.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.appAuthorization-2.1, \
  io.openliberty.xmlWS-4.0, \
  io.openliberty.appClientSupport-2.0, \
  io.openliberty.enterpriseBeans-4.0, \
  io.openliberty.messaging-3.1, \
  io.openliberty.messagingServer-3.0, \
  com.ibm.websphere.appserver.transaction-2.0
kind=ga
edition=base
