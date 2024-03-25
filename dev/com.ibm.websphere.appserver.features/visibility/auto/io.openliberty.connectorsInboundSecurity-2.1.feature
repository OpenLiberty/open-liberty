# Autofeature to enable connectors inbound security when connectors-2.1 and appSecurity-5.0 are enabled
-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectorsInboundSecurity-2.1
visibility=private
Subsystem-Name: Jakarta Connectors 2.1 Inbound Security
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.connectors-2.1))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.appSecurity-5.0)(osgi.identity=io.openliberty.appSecurity-6.0)(osgi.identity=io.openliberty.mpJwt-2.1)))"
IBM-Install-Policy: when-satisfied
-bundles=\
   io.openliberty.connectors.security.internal.inbound
kind=ga
edition=base
