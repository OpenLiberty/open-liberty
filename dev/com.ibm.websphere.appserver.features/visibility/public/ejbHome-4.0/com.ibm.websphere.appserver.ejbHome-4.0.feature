-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbHome-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: ejbHome-4.0
Subsystem-Name: Jakarta Enterprise Beans Home Interfaces 4.0
-features=com.ibm.websphere.appserver.ejbLite-4.0, \
 com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.ejb2x
kind=noship
edition=full
