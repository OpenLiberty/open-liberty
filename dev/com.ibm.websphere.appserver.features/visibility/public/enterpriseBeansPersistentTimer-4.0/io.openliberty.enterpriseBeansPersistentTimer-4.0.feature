-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansPersistentTimer-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeansPersistentTimer-4.0
Subsystem-Name: Jakarta Enterprise Beans Persistent Timers 4.0
-features=io.openliberty.persistentExecutorSubset-2.0, \
 com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:=4.3, \
 io.openliberty.enterpriseBeansLite-4.0, \
 com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.timer.persistent.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel
