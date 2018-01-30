-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jpaContainer2.1-cdi1.2
visibility=private
IBM-App-ForceRestart: install, \
 uninstall
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jpaContainer-2.1))"
-bundles=com.ibm.ws.jpa.container.v21.cdi
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
