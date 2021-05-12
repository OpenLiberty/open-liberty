-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jdbc4.2.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=\
 com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1"
-bundles=\
 com.ibm.ws.jdbc,\
 com.ibm.ws.jdbc.4.1,\
 com.ibm.ws.jdbc.4.2
kind=ga
edition=core
WLP-Activation-Type: parallel
