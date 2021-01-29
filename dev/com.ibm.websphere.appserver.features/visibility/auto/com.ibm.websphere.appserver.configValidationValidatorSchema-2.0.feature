-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationValidatorSchema-2.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.mpOpenAPI-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.configValidationJDBC-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationJCA-1.0)(osgi.identity=com.ibm.websphere.appserver.configValidationCloudant-1.0)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 io.openliberty.rest.handler.validator.openapi.2.0
kind=beta
edition=core
