-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi4.1-concurrent3.1
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-4.1)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.concurrent-3.1)))"
-bundles=\
  io.openliberty.concurrent.internal.cdi
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
