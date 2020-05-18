-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = com.ibm.wsspi.appserver.app.manager.wab.jakarta-1.0
visibility = private

-bundles= com.ibm.ws.app.manager.wab.jakarta; start-phase:=APPLICATION_EARLY

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.wsspi.appserver.webBundle-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

kind=noship
edition=full
WLP-Activation-Type: parallel
