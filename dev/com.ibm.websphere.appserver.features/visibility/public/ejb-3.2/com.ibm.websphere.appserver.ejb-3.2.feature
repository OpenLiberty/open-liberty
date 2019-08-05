-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejb-3.2
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejb-3.2
Subsystem-Name: Enterprise JavaBeans 3.2
Subsystem-Category: JavaEE7Application
-features=com.ibm.websphere.appserver.ejbPersistentTimer-3.2, \
 com.ibm.websphere.appserver.ejbHome-3.2, \
 com.ibm.websphere.appserver.ejbLite-3.2, \
 com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2, 4.3", \
 com.ibm.websphere.appserver.mdb-3.2, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.ejbRemote-3.2
kind=ga
edition=base
WLP-Activation-Type: parallel
