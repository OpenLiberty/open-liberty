-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi3.0-servlet5.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-3.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
-bundles=com.ibm.ws.cdi.2.0.web.jakarta, \
 com.ibm.ws.cdi.web.jakarta
-features=com.ibm.websphere.appserver.jakarta.jsp-3.0; apiJar=false
IBM-Install-Policy: when-satisfied
kind=noship
edition=full
WLP-Activation-Type: parallel
