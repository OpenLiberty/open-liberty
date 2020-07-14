-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jaccEjb-2.0
IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.ejbCore-2.0))", \
 osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.jacc-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=io.openliberty.security.authorization.internal.jacc.ejb
kind=noship
edition=full
