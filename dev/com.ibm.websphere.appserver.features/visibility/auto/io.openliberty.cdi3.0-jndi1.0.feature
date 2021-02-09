-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-jndi1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jndi-1.0))"
-bundles=com.ibm.ws.cdi.jndi.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
WLP-Activation-Type: parallel
