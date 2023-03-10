-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpGraphQL1.0-metrics3.0
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpGraphQL-1.0)(osgi.identity=io.openliberty.mpGraphQL-2.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpMetrics-3.0)(osgi.identity=io.openliberty.mpMetrics-4.0)))"
-bundles=io.openliberty.microprofile.graphql.internal.metrics.3.0
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
