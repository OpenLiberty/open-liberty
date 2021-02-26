-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-appSecurityClient
visibility=private
IBM-Process-Types: client, \
 server
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurityClient-1.0))"
-bundles=\
  com.ibm.ws.cdi.client
IBM-Install-Policy: when-satisfied
kind=beta
edition=base
