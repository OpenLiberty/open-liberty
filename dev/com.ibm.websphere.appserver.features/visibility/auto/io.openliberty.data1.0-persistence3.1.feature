-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.data1.0-persistence3.1
#TODO temporarily using EE 10 version of Jakarta Persistence. Expect this to change for EE 11
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.data-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.persistence-3.1))"
-bundles=\
  io.openliberty.data.internal.persistence
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
