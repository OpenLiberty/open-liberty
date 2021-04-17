-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistentExecutor.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=\
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.concurrent-1.0, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2, 4.3", \
  com.ibm.websphere.appserver.persistentExecutorSubset-1.0
kind=noship
edition=full
WLP-Activation-Type: parallel
