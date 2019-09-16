-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.configValidationJDBC-1.0
visibility=private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.jdbc-4.1)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.2)(osgi.identity=com.ibm.websphere.appserver.jdbc-4.3)))"
IBM-Install-Policy: when-satisfied
-bundles=\
 com.ibm.ws.rest.handler.validator,\
 com.ibm.ws.rest.handler.validator.jdbc
kind=ga
edition=core
