-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwtPropagation-2.1
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.1)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpJwt-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.org.eclipse.microprofile.jwt.2.1; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.jwt:microprofile-jwt-auth-api:2.1-RC2",\
 com.ibm.ws.security.mp.jwt.propagation, \
 io.openliberty.org.jboss.resteasy.common.jakarta, \
 io.openliberty.restfulWS.internal.globalhandler
kind=noship
edition=full
