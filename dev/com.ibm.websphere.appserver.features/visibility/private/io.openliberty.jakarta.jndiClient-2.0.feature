-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jakarta.jndiClient-2.0
visibility=private
-features=com.ibm.websphere.appserver.clientContainerRemoteSupportCommon-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.jakarta.cdi-3.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.jndi.remote.client
kind=beta
edition=base
