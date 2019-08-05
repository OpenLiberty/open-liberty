-include= ~${workspace}/cnf/resources/bnd/feature.props

# Functionality to support remote ejb client container - primarily assisting in EJB auto-link lookups.

symbolicName=com.ibm.websphere.appserver.ejbRemoteClientServer-1.0
visibility=private
IBM-API-Package: com.ibm.ws.clientcontainer.remote.common;type="internal"
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.ejbRemote-3.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0
-bundles=com.ibm.ws.ejbcontainer.remote.client.server
IBM-Process-Types: server
kind=ga
edition=base
WLP-Activation-Type: parallel
