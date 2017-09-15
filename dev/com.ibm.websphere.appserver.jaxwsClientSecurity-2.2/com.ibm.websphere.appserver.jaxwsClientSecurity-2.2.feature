-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsClientSecurity-2.2
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jaxwsClient-2.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.jaxws.clientcontainer.security
kind=ga
edition=base
