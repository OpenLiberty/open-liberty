-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationJCA-1.0
visibility=private
#TODO: Try re-enable schema testing in the CheckProfileZipsTest.checkBetaZip() test once we switch to using this auto feature.
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jca-1.7)(osgi.identity=com.ibm.websphere.appserver.jca-1.6)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.validator,\
 com.ibm.ws.rest.handler.validator.jca
kind=ga
edition=base
