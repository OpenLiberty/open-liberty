-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jcaSecurity.internal.ee-9.0
singleton=true
-features=com.ibm.websphere.appserver.transaction-2.0; ibm.tolerates:="2.1"
-bundles=com.ibm.ws.security.jca.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
