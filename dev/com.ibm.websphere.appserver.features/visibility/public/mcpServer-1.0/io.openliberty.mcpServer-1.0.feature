-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mcpServer-1.0
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
 io.openliberty.mcp.annotations, \
 io.openliberty.mcp.content, \
 io.openliberty.mcp.encoders, \
 io.openliberty.mcp.messaging, \
 io.openliberty.mcp.meta, \
 io.openliberty.mcp.tools
IBM-ShortName: mcpServer-1.0
Subsystem-Name: Model Context Protocol Server 1.0
-features=com.ibm.websphere.appserver.servlet-6.0;ibm.tolerates:=6.1,\
 io.openliberty.cdi-4.0;ibm.tolerates:=4.1,\
 io.openliberty.jsonb-3.0
-bundles=io.openliberty.mcp; location:="dev/api/ibm/", \
 io.openliberty.mcp.internal
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true