-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi4.0-concurrent3.0
visibility=private
#TODO remove osgi.identity=io.openliberty.cdi-3.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-4.0)(osgi.identity=io.openliberty.cdi-3.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.concurrent-3.0))"
-bundles=\
  io.openliberty.concurrent.internal.cdi.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
