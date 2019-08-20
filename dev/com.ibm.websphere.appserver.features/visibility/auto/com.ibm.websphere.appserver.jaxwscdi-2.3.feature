-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwscdi-2.3
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.3))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.cdi-1.2)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0)))"
IBM-Install-Policy: when-satisfied
-features=com.ibm.websphere.appserver.jndi-1.0
-bundles=com.ibm.ws.jaxws.cdi.2.3
kind=noship
edition=full
