-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi4.1-servlet6.1
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-4.1))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.servlet.internal-6.1))"
-bundles=io.openliberty.cdi.4.0.internal.web, \
 com.ibm.ws.cdi.web.jakarta
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
WLP-Activation-Type: parallel
