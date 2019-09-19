-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationValidatorSchema-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.0)(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-1.1)))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.configValidationJDBC-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationJCA-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationCloudant-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.validator.openapi
kind=ga
edition=core
