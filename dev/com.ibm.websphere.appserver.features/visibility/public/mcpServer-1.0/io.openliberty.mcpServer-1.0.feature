-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcpServer-1.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
 io.openliberty.mcp.annotations; type="ibm-api", \
 io.openliberty.mcp.features; type="ibm-api", \
 io.openliberty.mcp.tools; type="ibm-api", \
 org.mcpjava.server; type="stable", \
 org.mcpjava.server.completion; type="stable", \
 org.mcpjava.server.content; type="stable", \
 org.mcpjava.server.elicitation; type="stable", \
 org.mcpjava.server.progress; type="stable", \
 org.mcpjava.server.prompts; type="stable", \
 org.mcpjava.server.resources; type="stable", \
 org.mcpjava.server.roots; type="stable", \
 org.mcpjava.server.sampling; type="stable", \
 org.mcpjava.server.spi; type="stable", \
 org.mcpjava.server.tools; type="stable"
IBM-ShortName: mcpServer-1.0
Subsystem-Name: Model Context Protocol Server 1.0
-features=io.openliberty.mcpServer1.0.ee-10.0;ibm.tolerates:="11.0, 12.0"
-bundles=io.openliberty.mcp; location:="dev/api/ibm/,lib/", \
 io.openliberty.mcp.internal, \
 io.openliberty.org.mcp-java.mcp-server-api; location:="dev/api/stable/,lib/"; mavenCoordinates="org.mcp-java:mcp-server-api:1.0.0-Beta3", \
 io.openliberty.org.mcp-java.mcp-server-api.fragment
-files=dev/api/ibm/javadoc/io.openliberty.mcp_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
