-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakartaee-12.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-12.0
Subsystem-Version: 12.0.0
Subsystem-Name: Jakarta EE Platform 12.0
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.mail-2.2, \
  io.openliberty.messagingClient-3.0, \
  io.openliberty.connectors-2.2, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.batch-2.2, \
  io.openliberty.cdi-5.0, \
  io.openliberty.webProfile-12.0, \
  io.openliberty.messagingSecurity-3.0, \
  io.openliberty.appAuthorization-3.0, \
  io.openliberty.appClientSupport-2.0, \
  io.openliberty.enterpriseBeansRemote-4.0, \
  io.openliberty.enterpriseBeansPersistentTimer-4.0, \
  io.openliberty.mdb-4.0, \
  io.openliberty.messaging-3.1, \
  io.openliberty.messagingServer-3.0, \
  com.ibm.websphere.appserver.transaction-2.1
kind=noship
edition=full
