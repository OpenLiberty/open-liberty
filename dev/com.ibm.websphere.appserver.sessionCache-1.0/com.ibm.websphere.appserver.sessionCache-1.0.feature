-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionCache-1.0
visibility=public
IBM-ShortName: sessionCache-1.0
Manifest-Version: 1.0
Subsystem-Name: JCache Session Persistence
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.javax.servlet-4.0; ibm.tolerates:="3.1,3.0"; apiJar=false, \
 com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:=1.1, \
 com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.1, 4.0"
-bundles=com.ibm.websphere.javaee.jcache.1.1, \
 com.ibm.websphere.security, \
 com.ibm.ws.serialization, \
 com.ibm.ws.session
 com.ibm.ws.session.db, \
 com.ibm.ws.session.store
kind=noship
edition=full
