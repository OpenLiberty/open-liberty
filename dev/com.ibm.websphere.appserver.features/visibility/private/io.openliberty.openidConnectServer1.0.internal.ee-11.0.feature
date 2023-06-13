-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectServer1.0.internal.ee-11.0
singleton=true
visibility = private
-features=com.ibm.websphere.appserver.servlet-6.1, \
  io.openliberty.pages-4.0, \
  io.openliberty.jakarta.cdi-4.1; apiJar=false
-bundles=\
  io.openliberty.security.common.internal, \
  io.openliberty.security.openidconnect.internal.clients.common, \
  io.openliberty.security.openidconnect.internal.server, \
  io.openliberty.security.oidcclientcore.internal.jakarta
kind=noship
edition=full
