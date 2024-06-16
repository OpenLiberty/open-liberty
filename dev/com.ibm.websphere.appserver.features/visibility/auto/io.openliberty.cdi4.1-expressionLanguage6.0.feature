-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.cdi4.1-expressionLanguage6.0
Manifest-Version: 1.0
IBM-API-Package: jakarta.enterprise.inject.spi.el;  type="spec"
IBM-Provision-Capability: \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.cdi-4.1))", \
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.expressionLanguage-6.0))"
IBM-Install-Policy: when-satisfied
 kind=ga
edition=core
