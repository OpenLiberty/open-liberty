-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.batchManagement1.0-jms2.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.internal.jms-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.batchManagement-1.0))"
-features=com.ibm.websphere.appserver.transaction-1.2, \
  com.ibm.websphere.appserver.jca-1.7, \
  com.ibm.websphere.appserver.mdb-3.2
-bundles=com.ibm.ws.jbatch.jms
IBM-Install-Policy: when-satisfied
kind=ga
edition=base
