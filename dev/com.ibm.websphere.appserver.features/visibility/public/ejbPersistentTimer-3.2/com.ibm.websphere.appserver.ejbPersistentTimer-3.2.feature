-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbPersistentTimer-3.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbPersistentTimer-3.2
Subsystem-Name: Enterprise JavaBeans Persistent Timers 3.2
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.ejbLite-3.2, \
  com.ibm.websphere.appserver.persistentExecutorSubset-1.0
-bundles=com.ibm.ws.ejbcontainer.timer.persistent
kind=ga
edition=base
