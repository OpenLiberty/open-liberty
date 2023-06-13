-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.enterpriseBeansRemote-appSecurity
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.enterpriseBeansRemote-4.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appSecurity-4.0)(osgi.identity=io.openliberty.appSecurity-5.0)(osgi.identity=io.openliberty.appSecurity-6.0)(osgi.identity=io.openliberty.mpJwt-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=\
  com.ibm.ws.security.csiv2.common, \
  com.ibm.ws.security.csiv2
kind=ga
edition=base
