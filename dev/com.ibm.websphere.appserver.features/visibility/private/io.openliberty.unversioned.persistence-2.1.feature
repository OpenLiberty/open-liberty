-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.persistence-2.1
visibility=private
singleton=true
-features= \
  io.openliberty.jakartaPlatform.internal-7.0,\
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.2,4.3", \
  com.ibm.websphere.appserver.jpa-2.1
kind=noship
edition=full