-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectionManager1.0.internal.ee-9.0
singleton=true
-features=\
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.jakarta.connectors-2.0
-bundles=com.ibm.ws.jca.cm.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel