-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsweb-2.3
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxws-2.3))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.2.3.web, \
 com.ibm.ws.jaxws.2.3.webcontainer
kind=noship
edition=full
