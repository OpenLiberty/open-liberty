-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansPersistentTimer-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  com.ibm.websphere.persistence.mbean; type="ibm-api"
IBM-ShortName: enterpriseBeansPersistentTimer-4.0
WLP-AlsoKnownAs: ejbPersistentTimer-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0 Persistent Timers
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  io.openliberty.enterpriseBeansLite-4.0, \
  io.openliberty.persistentExecutorSubset-2.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.timer.persistent.jakarta
kind=ga
edition=base
WLP-Activation-Type: parallel
