-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectionManager1.0.internal.ee-10.0
singleton=true
-features=com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jakarta.connectors-2.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=com.ibm.ws.jca.cm.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
