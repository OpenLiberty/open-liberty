-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansHome-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeansHome-4.0
WLP-AlsoKnownAs: ejbHome-4.0
Subsystem-Name: Jakarta Enterprise Beans 4.0 Home Interfaces
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.enterpriseBeansLite-4.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.ejb2x
kind=beta
edition=base
WLP-Activation-Type: parallel
