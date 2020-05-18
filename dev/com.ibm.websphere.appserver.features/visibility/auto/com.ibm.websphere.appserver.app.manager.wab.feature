-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = com.ibm.wsspi.appserver.app.manager.wab-1.0
visibility = private

-bundles= com.ibm.ws.app.manager.wab; start-phase:=APPLICATION_EARLY

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.wsspi.appserver.webBundle-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"

IBM-Install-Policy: when-satisfied

edition=full
kind=ga
WLP-Activation-Type: parallel
