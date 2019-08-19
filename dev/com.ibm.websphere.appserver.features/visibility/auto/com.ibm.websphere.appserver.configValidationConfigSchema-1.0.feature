-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationConfigSchema-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.0)(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.1)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.config.openapi
kind=ga
edition=core
