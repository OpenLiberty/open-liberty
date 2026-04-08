-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcp.internal.mptelemetry-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpTelemetry-*))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mcpServer-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.mcp.internal.mptelemetry
kind=ga
edition=core