-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundleSecurity.jakarta-1.0
visibility = private

-bundles= io.openliberty.webcontainer.security.internal; start-phase:=SERVICE_EARLY, \
          io.openliberty.security.authentication.internal.tai

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.wsspi.appserver.webBundleSecurity-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

edition=full
kind=noship
WLP-Activation-Type: parallel
