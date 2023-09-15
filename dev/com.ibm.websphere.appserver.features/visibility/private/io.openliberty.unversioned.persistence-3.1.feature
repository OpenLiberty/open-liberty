-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.persistence-3.1
visibility=private
singleton=true
-features= \
  io.openliberty.jakartaPlatform.internal-10.0,\
  com.ibm.websphere.appserver.transaction-2.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.persistence-3.1
kind=noship
edition=full