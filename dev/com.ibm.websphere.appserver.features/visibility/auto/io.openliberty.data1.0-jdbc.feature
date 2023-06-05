-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data1.0-jdbc
#TODO temporarily using EE 10 versions. Expect this to change for EE 11
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.data-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jdbc-4.3)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2)))"
-features=\
  com.ibm.websphere.appserver.transaction-2.0,\
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3",\
  io.openliberty.persistenceService-2.0
-bundles=\
  io.openliberty.data.internal.persistence
IBM-Install-Policy: when-satisfied
kind=beta
edition=base
