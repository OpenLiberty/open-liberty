-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-faces3.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.faces-3.0))"
-bundles=com.ibm.ws.cdi.2.0.jsf.jakarta
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
