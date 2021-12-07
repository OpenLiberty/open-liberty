-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batchManagement1.0.internal.ee-10.0
singleton=true
-features=com.ibm.websphere.appserver.servlet-6.0, \
  io.openliberty.batch-2.1, \
  com.ibm.websphere.appserver.transaction-2.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.jsonp-2.1
-bundles=com.ibm.ws.jbatch.joblog.jakarta, \
  com.ibm.ws.jbatch.rest.jakarta
kind=noship
edition=full
