-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jdbc4.0.internal.ee-6.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=\
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2"
-bundles=com.ibm.ws.jdbc
kind=ga
edition=core
