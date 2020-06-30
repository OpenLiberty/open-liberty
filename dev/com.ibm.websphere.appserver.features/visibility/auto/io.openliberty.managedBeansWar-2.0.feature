-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.managedBeansWar-2.0
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.managedBeansCore-2.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.ejbcontainer.war
kind=noship
edition=full
WLP-Activation-Type: parallel
