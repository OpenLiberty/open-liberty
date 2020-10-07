-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwtPropagation-1.2
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jaxrsClient-2.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpJwt-1.2)))"
IBM-Install-Policy: when-satisfied 
-bundles=io.openliberty.org.eclipse.microprofile.jwt.1.2; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:1.2-RC1",\
 com.ibm.ws.security.mp.jwt.propagation, \
 com.ibm.ws.jaxrs.2.0.client
kind=ga
edition=core
