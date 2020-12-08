-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.adminCenter.tool.javaBatch-1.0
visibility=private
IBM-ShortName: javaBatch-1.0
Subsystem-Name: Java Batch
Subsystem-Category: adminCenter
Subsystem-Version: 1.0.0
Subsystem-Icon: OSGI-INF/javaBatch_142.png, OSGI-INF/javaBatch_78.png; size=78, OSGI-INF/javaBatch_142.png; size=142
-bundles=com.ibm.ws.ui.tool.javaBatch
kind=ga
edition=base
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.adminCenter-1.0))",
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.batchManagement-1.0))"
IBM-Install-Policy: when-satisfied
IBM-Feature-Version: 2