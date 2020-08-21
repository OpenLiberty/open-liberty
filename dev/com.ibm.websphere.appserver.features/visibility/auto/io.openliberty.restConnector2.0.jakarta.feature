-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restConnector2.0.jakarta
visibility = private

-bundles= com.ibm.ws.jmx.connector.server.rest.jakarta

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restConnector-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

kind=beta
edition=core
