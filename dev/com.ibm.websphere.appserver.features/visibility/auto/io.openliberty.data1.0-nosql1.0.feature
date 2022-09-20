-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data1.0-nosql1.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.data-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.nosql-1.0))"
-bundles=\
  io.openliberty.data.internal.nosql
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
