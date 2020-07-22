-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restConnector.javaee-2.0
visibility = private

-bundles= com.ibm.ws.jmx.connector.server.rest

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"

IBM-Install-Policy: when-satisfied

edition=core
kind=ga
WLP-Activation-Type: parallel
