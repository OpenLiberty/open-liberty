-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.unversioned.persistence-2.2
visibility=private
singleton=true
-features= \
  io.openliberty.jakartaPlatform.internal-8.0,\
  com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.jpa-2.2
kind=noship
edition=full