-include= ~${workspace}/cnf/resources/bnd/feature.props

# Functionality to support remote client container - primarily assisting in JNDI operations.

symbolicName=com.ibm.websphere.appserver.clientContainerRemoteSupport-1.0
visibility=private
IBM-API-Package: com.ibm.ws.clientcontainer.remote.common;type="internal"
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.iioptransport-1.0, \
 com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0
-bundles=com.ibm.ws.clientcontainer.remote.server
IBM-Process-Types: server
kind=ga
edition=base
WLP-Activation-Type: parallel
