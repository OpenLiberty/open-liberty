-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.cdi1.2-servlet4.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.cdi-1.2))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0))"
-bundles=com.ibm.ws.cdi.1.2.web, \
 com.ibm.websphere.javaee.jsp.2.3; location:="dev/api/spec/,lib/"
IBM-Install-Policy: when-satisfied
kind=beta
edition=core
