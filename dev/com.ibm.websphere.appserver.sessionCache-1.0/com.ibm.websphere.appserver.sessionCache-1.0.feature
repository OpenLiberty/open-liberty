-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sessionCache-1.0
visibility=public
IBM-ShortName: sessionCache-1.0
Manifest-Version: 1.0
Subsystem-Name: JCache Session Persistence
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.javax.servlet-4.0; apiJar=false, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.jdbc-4.2
-bundles=com.ibm.websphere.javaee.jcache.1.1, \
 com.ibm.websphere.security, \
 com.ibm.ws.session.db, \
 com.ibm.ws.serialization, \
 com.ibm.ws.session
kind=noship
edition=full
