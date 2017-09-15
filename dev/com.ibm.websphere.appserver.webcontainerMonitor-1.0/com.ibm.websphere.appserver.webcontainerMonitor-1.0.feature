-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.webcontainerMonitor-1.0
Manifest-Version: 1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.monitor-1.0))", \
 osgi.identity; filter:="(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.webcontainer.monitor
kind=ga
edition=core
