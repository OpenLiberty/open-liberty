-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = io.openliberty.webBundleSecurity1.0.jakarta
visibility = private

-bundles= io.openliberty.webcontainer.security.internal; start-phase:=SERVICE_EARLY, \
          io.openliberty.security.authentication.internal.filter, \
          io.openliberty.security.authentication.internal.tai, \
          io.openliberty.security.sso.internal

IBM-Provision-Capability: osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.wsspi.appserver.webBundleSecurity-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"

IBM-Install-Policy: when-satisfied

edition=core
kind=beta
WLP-Activation-Type: parallel
