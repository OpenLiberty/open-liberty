-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcp-1.0-monitor-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mcp-1.0))"
IBM-Install-Policy: when-satisfied
IBM-API-Package: \
 io.openliberty.mcp.monitor; type="ibm-api"
-bundles=io.openliberty.mcp.monitor; location:="dev/api/ibm/,lib/", \
 io.openliberty.mcp.internal.monitor
kind=beta
edition=core