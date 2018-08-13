-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwtSsoToken-1.0
visibility=private
IBM-ShortName: jwtSsoToken-1.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.appSecurity-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.jwtSso-1.0))"
IBM-Install-Policy: when-satisfied
Subsystem-Name: JwtSsoToken
 -features=com.ibm.websphere.appserver.appSecurity-2.0
-bundles=com.ibm.ws.security.jwtsso,\
  com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0",\
  com.ibm.ws.security.jwtsso.token
kind=ga
edition=core
