-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionDatabase-1.0
visibility=public
IBM-ShortName: sessionDatabase-1.0
Manifest-Version: 1.0
Subsystem-Name: Database Session Persistence 1.0
-features=com.ibm.websphere.appserver.sessionStore-1.0.0.Database, \
 com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1,4.0"; apiJar=false, \
 com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.jdbc-4.0; ibm.tolerates:="4.1, 4.2, 4.3"
-bundles=com.ibm.websphere.security, \
 com.ibm.ws.session.store, \
 com.ibm.ws.session.db, \
 com.ibm.ws.serialization, \
 com.ibm.ws.session
kind=ga
edition=core
