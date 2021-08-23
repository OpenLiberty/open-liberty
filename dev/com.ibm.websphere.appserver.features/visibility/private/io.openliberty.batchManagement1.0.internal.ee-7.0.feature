-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batchManagement1.0.internal.ee-7.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.batch-1.0, \
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.0,4.2,4.3"
-bundles=com.ibm.ws.jbatch.joblog, \
  com.ibm.ws.jbatch.rest
kind=ga
edition=base
