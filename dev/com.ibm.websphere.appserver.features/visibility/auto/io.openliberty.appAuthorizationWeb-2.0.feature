-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorizationWeb-2.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.webAppSecurity-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appAuthorization-2.0)(osgi.identity=io.openliberty.appAuthorization-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.security.authorization.internal.jacc.web
kind=ga
edition=core
