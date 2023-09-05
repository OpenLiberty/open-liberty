-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpGraphQL2.0-metrics5.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpGraphQL-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpMetrics-5.0))"
-bundles=io.openliberty.microprofile.graphql.internal.metrics.5.0
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
