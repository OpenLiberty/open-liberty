-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwtPropagation-2.0
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpJwt-2.0)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.org.eclipse.microprofile.jwt.2.0; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:2.0",\
 com.ibm.ws.security.mp.jwt.propagation, \
 io.openliberty.org.jboss.resteasy.common.jakarta, \
 io.openliberty.restfulWS.internal.globalhandler.jakarta
kind=ga
edition=core
