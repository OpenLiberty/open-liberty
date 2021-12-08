-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpGraphQL1.0-metrics2.3
visibility=private
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpGraphQL-1.0)(osgi.identity=io.openliberty.mpGraphQL-2.0)))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.3)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-3.0)(osgi.identity=io.openliberty.mpMetrics-4.0)))"
-bundles=com.ibm.ws.microprofile.graphql.metrics.1.0
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
