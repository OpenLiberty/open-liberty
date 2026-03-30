-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jdbc.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=\
 com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1"
-bundles=\
 com.ibm.ws.jdbc
kind=ga
edition=core
WLP-Activation-Type: parallel
