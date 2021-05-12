-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.configValidationJCA-2.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=io.openliberty.connectors-2.0))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.validator,\
 com.ibm.ws.rest.handler.validator.jca.jakarta
kind=beta
edition=base
WLP-Activation-Type: parallel
