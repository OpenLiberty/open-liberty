-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistentExecutor.internal.ee-9.0
singleton=true
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3,4.1", \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.concurrent-2.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.persistentExecutorSubset-2.0
kind=ga
edition=full
WLP-Activation-Type: parallel
