-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jdbc-4.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: com.ibm.wsspi.zos.tx; type="internal"
IBM-ShortName: jdbc-4.2
Subsystem-Name: Java Database Connectivity 4.2
-features=\
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.appLifecycle-1.0, \
 com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="1.1, 2.0", \
 com.ibm.websphere.appserver.connectionManagement-1.0, \
 com.ibm.websphere.appserver.requestProbes-1.0, \
 io.openliberty.jdbc4.2.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
 com.ibm.ws.jdbc.4.2.feature,\
 com.ibm.ws.jdbc.metatype
kind=ga
edition=core
WLP-Activation-Type: parallel
