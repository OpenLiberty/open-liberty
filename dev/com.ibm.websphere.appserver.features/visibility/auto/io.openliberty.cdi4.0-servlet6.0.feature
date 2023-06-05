-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi4.0-servlet6.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-4.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.servlet.internal-6.0))"
-bundles=io.openliberty.cdi.4.0.internal.web, \
 com.ibm.ws.cdi.web.jakarta
-features=io.openliberty.jakarta.pages-3.1; apiJar=false
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
