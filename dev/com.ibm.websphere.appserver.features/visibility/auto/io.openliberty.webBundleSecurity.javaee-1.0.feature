-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundleSecurity.javaee-1.0
visibility = private

-bundles= com.ibm.ws.webcontainer.security; start-phase:=SERVICE_EARLY, \
          com.ibm.ws.security.authentication.tai

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.wsspi.appserver.webBundleSecurity-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"

IBM-Install-Policy: when-satisfied

edition=core
kind=ga
WLP-Activation-Type: parallel
