-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminCenter.tool.explore-1.0
visibility=private
IBM-ShortName: explore-1.0
Subsystem-Name: Explore
Subsystem-Category: adminCenter
Subsystem-Version: 1.0.0
Subsystem-Icon: OSGI-INF/explore_142x142.png,OSGI-INF/explore_78x78.png;size=78,OSGI-INF/explore_142x142.png;size=142
-bundles=com.ibm.ws.ui.tool.explore
kind=ga
edition=base
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.adminCenter-1.0))"
IBM-Install-Policy: when-satisfied
IBM-Feature-Version: 2