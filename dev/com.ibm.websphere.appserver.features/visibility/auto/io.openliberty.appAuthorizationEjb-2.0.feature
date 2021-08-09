-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.appAuthorizationEjb-2.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.ejbCore-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.appAuthorization-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.security.authorization.internal.jacc.ejb
kind=beta
edition=core
