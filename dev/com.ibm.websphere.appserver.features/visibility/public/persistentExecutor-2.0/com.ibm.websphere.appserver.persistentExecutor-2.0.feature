-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: persistentExecutor-2.0
Subsystem-Name: Persistent Scheduled Executor 2.0
symbolicName=com.ibm.websphere.appserver.persistentExecutor-2.0
visibility=public
IBM-API-Package: \
  com.ibm.websphere.concurrent.persistent; type="ibm-api", \
  com.ibm.websphere.concurrent.persistent.mbean; type="ibm-api"
-features=\
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.concurrent-2.0, \
  com.ibm.websphere.appserver.jdbc-4.3; ibm.tolerates:="4.2, 4.1", \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.persistentExecutorSubset-2.0
kind=noship
edition=full
