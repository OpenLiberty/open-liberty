-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationConfigSchema-2.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
 io.openliberty.rest.handler.config.openapi.2.0,\
 io.openliberty.rest.handler.config.openapi.common
kind=beta
edition=core
