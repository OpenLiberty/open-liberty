-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: persistentExecutor-1.0
Subsystem-Name: Persistent Scheduled Executor 1.0
symbolicName=com.ibm.websphere.appserver.persistentExecutor-1.0
visibility=public
singleton=true
IBM-API-Package: \
  com.ibm.websphere.concurrent.persistent; type="ibm-api", \
  com.ibm.websphere.concurrent.persistent.mbean; type="ibm-api"
-features=\
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="2.0", \
  io.openliberty.persistentExecutor.internal.ee-7.0; ibm.tolerates:="9.0", \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2, 4.3"
kind=noship
edition=full
