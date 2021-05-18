-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansRemote-appSecurity
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.enterpriseBeansRemote-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appSecurity-4.0))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.security-1.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=\
  com.ibm.ws.security.csiv2.common, \
  com.ibm.ws.security.csiv2
kind=beta
edition=base
