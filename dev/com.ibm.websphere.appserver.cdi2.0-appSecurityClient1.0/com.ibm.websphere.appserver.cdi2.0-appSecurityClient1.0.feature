-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi2.0-appSecurityClient1.0
visibility=private
IBM-Process-Types: client, \
 server
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))"
-bundles=com.ibm.ws.cdi.client
IBM-Install-Policy: when-satisfied
kind=noship
edition=base
