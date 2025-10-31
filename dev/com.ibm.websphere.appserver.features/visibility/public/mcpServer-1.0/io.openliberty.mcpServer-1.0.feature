-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcpServer-1.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
 io.openliberty.mcp.annotations; type="ibm-api", \
 io.openliberty.mcp.content; type="ibm-api", \
 io.openliberty.mcp.encoders; type="ibm-api", \
 io.openliberty.mcp.messaging; type="ibm-api", \
 io.openliberty.mcp.meta; type="ibm-api", \
 io.openliberty.mcp.tools; type="ibm-api"
IBM-ShortName: mcpServer-1.0
Subsystem-Name: Model Context Protocol Server 1.0
-features=io.openliberty.mcpServer1.0.ee-10.0;ibm.tolerates:=11.0
-bundles=io.openliberty.mcp; location:="dev/api/ibm/,lib/", \
 io.openliberty.mcp.internal
-files=dev/api/ibm/javadoc/io.openliberty.mcp_1.0-javadoc.zip
kind=beta
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
