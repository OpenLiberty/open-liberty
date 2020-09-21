-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpJwtPropagation-1.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.0)(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpJwt-1.0)(osgi.identity=com.ibm.websphere.appserver.mpJwt-1.1)))"
IBM-Install-Policy: when-satisfied 
-bundles=com.ibm.websphere.org.eclipse.microprofile.jwt.1.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.0", \
 com.ibm.ws.security.mp.jwt.propagation, \
 com.ibm.ws.jaxrs.2.0.client
kind=ga
edition=core
