-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.openidConnectClient1.0.internal.ee-9.0
singleton=true
visibility = private
-features=\
  io.openliberty.jakarta.cdi-3.0; apiJar=false, \
  io.openliberty.pages-3.0, \
  com.ibm.websphere.appserver.servlet-5.0
-bundles=\
  io.openliberty.security.openidconnect.internal.client, \
  io.openliberty.security.openidconnect.internal.clients.common
kind=beta
edition=core
