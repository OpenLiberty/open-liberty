-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.iioptransport.transaction-2.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.transaction-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.iioptransport-1.0))"
-bundles=com.ibm.ws.transport.iiop.transaction.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=base
WLP-Activation-Type: parallel
