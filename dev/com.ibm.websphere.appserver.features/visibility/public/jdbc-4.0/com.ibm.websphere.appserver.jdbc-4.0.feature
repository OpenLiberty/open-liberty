-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jdbc-4.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.wsspi.zos.tx; type="internal"
IBM-ShortName: jdbc-4.0
Subsystem-Name: Java Database Connectivity 4.0
-features=com.ibm.websphere.appserver.javax.connector.internal-1.6; ibm.tolerates:=1.7, \
 com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.appLifecycle-1.0, \
 com.ibm.websphere.appserver.connectionManagement-1.0, \
 com.ibm.websphere.appserver.requestProbes-1.0
-bundles=com.ibm.ws.jdbc, \
 com.ibm.ws.jdbc.4.0.feature
kind=ga
edition=core
