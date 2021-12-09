-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jndiClient-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=private
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0, \
  com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.jndi-1.0
-bundles=com.ibm.ws.jndi.remote.client
kind=ga
edition=base
