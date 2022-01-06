-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-servlet5.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.cdi-3.0)(osgi.identity=io.openliberty.cdi-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-5.0)(osgi.identity=com.ibm.websphere.appserver.servlet-6.0)))"
-bundles=com.ibm.ws.cdi.2.0.web.jakarta, \
 com.ibm.ws.cdi.web.jakarta
-features=io.openliberty.jakarta.pages-3.0; apiJar=false; ibm.tolerates:="3.1"
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
