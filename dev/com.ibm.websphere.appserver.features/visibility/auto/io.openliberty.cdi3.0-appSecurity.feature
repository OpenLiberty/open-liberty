-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-appSecurity
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=io.openliberty.cdi-4.0)(osgi.identity=io.openliberty.cdi-4.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appSecurity-4.0)(osgi.identity=io.openliberty.appSecurity-5.0)(osgi.identity=io.openliberty.appSecurity-6.0)(osgi.identity=io.openliberty.mpJwt-2.1)))"
-bundles=\
  com.ibm.ws.cdi.security
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
