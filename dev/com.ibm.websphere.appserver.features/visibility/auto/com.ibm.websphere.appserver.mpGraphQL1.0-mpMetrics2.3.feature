-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpGraphQL1.0-mpMetrics2.3
visibility=private
singleton=true
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpGraphQL-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpMetrics-2.3))"
-bundles=com.ibm.ws.io.smallrye.graphql.metrics
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
