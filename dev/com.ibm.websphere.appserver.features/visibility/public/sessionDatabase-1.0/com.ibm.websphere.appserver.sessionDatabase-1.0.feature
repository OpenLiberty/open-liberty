-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionDatabase-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: sessionDatabase-1.0
Manifest-Version: 1.0
Subsystem-Name: Database Session Persistence 1.0
-features=com.ibm.websphere.appserver.sessionStore-1.0.0.Database, \
 com.ibm.websphere.appserver.jndi-1.0, \
 io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0,5.0"; apiJar=false, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2,2.0", \
 com.ibm.websphere.appserver.jdbc-4.0; ibm.tolerates:="4.1, 4.2, 4.3", \
 io.openliberty.sessionDatabase1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.serialization
kind=ga
edition=core
