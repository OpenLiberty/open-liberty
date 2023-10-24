-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batch2.1.internal.ee-10.0
singleton=true
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.persistence-3.1
kind=ga
edition=base
