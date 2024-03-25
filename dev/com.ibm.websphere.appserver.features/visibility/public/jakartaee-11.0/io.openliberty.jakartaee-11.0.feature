-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaee-11.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-11.0
Subsystem-Version: 11.0.0
Subsystem-Name: Jakarta EE Platform 11.0
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.mail-2.1, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.batch-2.1, \
  io.openliberty.cdi-4.1, \
  io.openliberty.webProfile-11.0, \
  io.openliberty.messagingSecurity-3.0, \
  io.openliberty.appAuthorization-3.0, \
  io.openliberty.appClientSupport-2.0, \
  io.openliberty.enterpriseBeansRemote-4.0, \
  io.openliberty.enterpriseBeansPersistentTimer-4.0, \
  io.openliberty.mdb-4.0, \
  io.openliberty.messaging-3.1, \
  io.openliberty.messagingServer-3.0, \
  com.ibm.websphere.appserver.transaction-2.0
kind=noship
edition=full
