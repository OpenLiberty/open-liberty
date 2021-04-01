-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectServer1.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.openidConnectServer-1.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
IBM-Install-Policy: when-satisfied
-features=\
  io.openliberty.jakarta.cdi-3.0; apiJar=false, \
  io.openliberty.pages-3.0, \
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  io.openliberty.security.common.internal, \
  io.openliberty.security.openidconnect.internal.clients.common, \
  io.openliberty.security.openidconnect.internal.server
kind=noship
edition=full
