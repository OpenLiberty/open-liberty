-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminCenter.tool.serverConfig-1.0
visibility=private
IBM-ShortName: serverConfig-1.0
Subsystem-Name: Server Config
Subsystem-Category: adminCenter
Subsystem-Version: 1.0.0
Subsystem-Icon: OSGI-INF/serverConfig_142x142.png,OSGI-INF/serverConfig_78x78.png;size=78,OSGI-INF/serverConfig_142x142.png;size=142
-bundles=com.ibm.ws.ui.tool.serverConfig
kind=ga
edition=base
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.adminCenter-1.0))"
IBM-Install-Policy: when-satisfied
IBM-Feature-Version: 2