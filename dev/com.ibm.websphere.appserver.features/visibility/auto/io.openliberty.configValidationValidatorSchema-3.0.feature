-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.configValidationValidatorSchema-3.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=io.openliberty.mpOpenAPI-3.0)(osgi.identity=io.openliberty.mpOpenAPI-3.1)))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.configValidationJDBC-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationJCA-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationCloudant-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 io.openliberty.rest.handler.validator.openapi.2.0.jakarta
kind=ga
edition=core
