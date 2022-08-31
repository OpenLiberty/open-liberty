-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.configValidationConfigSchema-3.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpOpenAPI-3.0)(osgi.identity=io.openliberty.mpOpenAPI-3.1)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 io.openliberty.rest.handler.config.openapi.2.0,\
 io.openliberty.rest.handler.config.openapi.common.jakarta
kind=ga
edition=core
