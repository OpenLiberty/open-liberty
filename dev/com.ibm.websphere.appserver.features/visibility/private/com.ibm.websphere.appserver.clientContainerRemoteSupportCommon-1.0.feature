-include= ~${workspace}/cnf/resources/bnd/feature.props

# Functionality to support remote client container - primarily assisting in JNDI operations.

symbolicName=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0
visibility=private
-features=com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.iiopclient-1.0
-bundles=com.ibm.ws.clientcontainer.remote.common
kind=ga
edition=base
WLP-Activation-Type: parallel
