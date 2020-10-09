-include= ~${workspace}/cnf/resources/bnd/feature.props

# Functionality to support remote enterprise beans client container - primarily assisting in enterprise beans auto-link lookups.

symbolicName=io.openliberty.enterpriseBeansRemoteClientServer-2.0
visibility=private
IBM-API-Package: com.ibm.ws.clientcontainer.remote.common;type="internal"
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.enterpriseBeansRemote-4.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0
-bundles=com.ibm.ws.ejbcontainer.remote.client.server.jakarta
IBM-Process-Types: server
kind=beta
edition=base
WLP-Activation-Type: parallel
