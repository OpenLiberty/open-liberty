-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.ejbRemote-3.2
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: com.ibm.ws.ejb.portable; type="internal"
IBM-ShortName: ejbRemote-3.2
Subsystem-Name: Enterprise JavaBeans Remote 3.2
-features=com.ibm.websphere.appserver.iioptransport-1.0, \
 com.ibm.websphere.appserver.ejbLite-3.2, \
 com.ibm.websphere.appserver.transaction-1.2
-bundles=com.ibm.ws.ejbcontainer.remote
-files=clients/ejbRemotePortable.jar
kind=ga
edition=base
WLP-Activation-Type: parallel
