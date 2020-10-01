-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansRemote-4.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.ws.ejb.portable; type="internal"
IBM-ShortName: enterpriseBeansRemote-4.0
IBM-AlsoKnownAs: ejbRemote-4.0
Subsystem-Name: Jakarta Enterprise Beans Remote 4.0
-features=com.ibm.websphere.appserver.iioptransport-1.0, \
 io.openliberty.enterpriseBeansLite-4.0, \
 com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.ejbcontainer.remote.jakarta
-files=clients/ejbRemotePortable.jakarta.jar
kind=beta
edition=base
WLP-Activation-Type: parallel
