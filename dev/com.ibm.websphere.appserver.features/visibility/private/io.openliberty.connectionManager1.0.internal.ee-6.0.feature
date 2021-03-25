-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectionManager1.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=\
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2", \
 com.ibm.websphere.appserver.javax.connector.internal-1.6; ibm.tolerates:=1.7
-bundles=com.ibm.ws.jca.cm
kind=ga
edition=core
WLP-Activation-Type: parallel
