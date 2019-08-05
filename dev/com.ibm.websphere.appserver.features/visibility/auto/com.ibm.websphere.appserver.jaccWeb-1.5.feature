-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaccWeb-1.5
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.webAppSecurity-1.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jacc-1.5))"
IBM-Install-Policy: when-satisfied
-bundles=com.ibm.ws.security.authorization.jacc.web, \
 com.ibm.websphere.javaee.jacc.1.5; location:="dev/api/spec/,lib"; mavenCoordinates="javax.security.jacc:javax.security.jacc-api:1.5"
kind=ga
edition=core
WLP-Activation-Type: parallel
