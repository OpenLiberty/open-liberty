-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data-ejb
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.data-1.1)(osgi.identity=io.openliberty.data-1.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.enterpriseBeansLite-4.0))"
-bundles=\
  io.openliberty.data.internal.ejb
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
