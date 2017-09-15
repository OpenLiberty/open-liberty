-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.managedBeansWar-1.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.managedBeansCore-1.0))", \
 osgi.identity; filter:="(|(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.0))(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.ejbcontainer.war
kind=ga
edition=core
