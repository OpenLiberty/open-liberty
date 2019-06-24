-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationMpOpenAPI-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-3.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.0)(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.1)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.validator,\
 com.ibm.ws.rest.handler.validator.openapi
kind=beta
edition=base
