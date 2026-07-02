-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectClient1.0.internal.ee-10.0
singleton=true
visibility = private
-features=com.ibm.websphere.appserver.servlet-6.0, \
  io.openliberty.pages-3.1, \
  io.openliberty.jakarta.cdi-4.0; apiJar=false
-bundles=\
  io.openliberty.security.openidconnect.internal.client, \
  io.openliberty.security.openidconnect.internal.clients.common, \
  io.openliberty.security.oidcclientcore.internal.jakarta, \
  io.openliberty.security.oidcclient.wellknown.internal.jakarta
kind=ga
edition=core
