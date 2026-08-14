-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data1.1-jdbc
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.data-1.1))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jdbc-4.3)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2)))"
# TODO:  When data 1.1 no longer tolerates EE 11, transaction dependency should just be 2.1
-features=\
  com.ibm.websphere.appserver.transaction-2.0; ibm.tolerates:="2.1",\
  com.ibm.websphere.appserver.jdbc-4.3; ibm.tolerates:="4.2",\
  io.openliberty.persistenceService-2.0
-bundles=\
  io.openliberty.data.internal
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
