-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi2.0-servlet4.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0))"
-bundles=com.ibm.ws.cdi.2.0.web, \
 com.ibm.ws.cdi.web, \
 com.ibm.websphere.javaee.jsp.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.servlet.jsp:javax.servlet.jsp-api:2.3.1"
IBM-Install-Policy: when-satisfied
kind=ga
edition=core
WLP-Activation-Type: parallel
