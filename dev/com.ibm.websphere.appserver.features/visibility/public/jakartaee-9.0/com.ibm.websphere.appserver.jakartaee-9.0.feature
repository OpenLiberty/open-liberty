-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jakartaee-9.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: jakartaee-9.0
Subsystem-Version: 9.0.0
Subsystem-Name: Jakarta EE Platform 9.0
-features=\
 com.ibm.websphere.appserver.concurrent-2.0,\
 com.ibm.websphere.appserver.transaction-2.0,\
 com.ibm.websphere.appserver.webProfile-9.0,\
 io.openliberty.ejbHome-4.0
kind=beta
edition=base
