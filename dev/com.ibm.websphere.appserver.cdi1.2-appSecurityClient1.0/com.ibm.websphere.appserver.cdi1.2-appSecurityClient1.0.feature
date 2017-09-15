-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi1.2-appSecurityClient1.0
visibility=private
IBM-Process-Types: client, \
 server
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))"
-bundles=com.ibm.ws.cdi.1.2.client
IBM-Install-Policy: when-satisfied
kind=ga
edition=base
