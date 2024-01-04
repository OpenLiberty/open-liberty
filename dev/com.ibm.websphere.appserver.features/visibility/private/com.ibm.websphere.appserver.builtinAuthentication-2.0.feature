-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.builtinAuthentication-2.0
Subsystem-Version: 2.0.0
singleton=true
-features=io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0, 6.1", \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  com.ibm.websphere.appserver.ltpa-1.0, \
  io.openliberty.jcache.internal-1.1
-bundles=\
  com.ibm.ws.security.authentication, \
  com.ibm.ws.security.credentials.wscred, \
  com.ibm.websphere.security, \
  io.openliberty.security.jaas.internal.common, \
  io.openliberty.security.authentication.internal.builtin, \
  io.openliberty.security.authentication.internal.tai, \
  com.ibm.ws.security.mp.jwt.proxy,\
  com.ibm.ws.security.kerberos.auth
kind=ga
edition=core
