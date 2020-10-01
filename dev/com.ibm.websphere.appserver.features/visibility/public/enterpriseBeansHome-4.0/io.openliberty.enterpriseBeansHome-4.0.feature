-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansHome-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: enterpriseBeansHome-4.0
IBM-AlsoKnownAs: ejbHome-4.0
Subsystem-Name: Jakarta Enterprise Beans Home Interfaces 4.0
-features=io.openliberty.enterpriseBeansLite-4.0, \
 com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.ejb2x
kind=beta
edition=base
WLP-Activation-Type: parallel
