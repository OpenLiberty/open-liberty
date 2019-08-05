-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbPersistentTimer-3.2
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbPersistentTimer-3.2
Subsystem-Name: Enterprise JavaBeans Persistent Timers 3.2
-features=com.ibm.websphere.appserver.persistentExecutorSubset-1.0, \
 com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2, 4.3", \
 com.ibm.websphere.appserver.ejbLite-3.2, \
 com.ibm.websphere.appserver.transaction-1.2
-bundles=com.ibm.ws.ejbcontainer.timer.persistent
kind=ga
edition=base
WLP-Activation-Type: parallel
