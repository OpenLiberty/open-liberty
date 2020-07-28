-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.restHandler1.0.jakarta
visibility = private

-bundles= com.ibm.ws.rest.handler.jakarta

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.restHandler-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

edition=full
kind=noship
WLP-Activation-Type: parallel
