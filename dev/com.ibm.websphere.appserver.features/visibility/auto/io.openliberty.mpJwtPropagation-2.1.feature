-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpJwtPropagation-2.1
visibility=private
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.restfulWSClient-3.1)(osgi.identity=io.openliberty.restfulWSClient-4.0)))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.mpJwt-2.1))"
IBM-Install-Policy: when-satisfied
-bundles= \
 com.ibm.ws.security.mp.jwt.propagation, \
 io.openliberty.restfulWS.internal.globalhandler, \
 io.openliberty.webservices.handler
kind=ga
edition=core
