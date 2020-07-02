-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.builtinAuthentication-2.0
-features=\
  io.openliberty.jakarta.servlet-5.0; apiJar=false, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.ltpa-1.0
-bundles=\
  com.ibm.ws.security.authentication, \
  com.ibm.ws.security.credentials.wscred, \
  com.ibm.websphere.security, \
  io.openliberty.security.jaas.internal.common, \
  io.openliberty.security.authentication.internal.builtin, \
  com.ibm.ws.security.mp.jwt.proxy,\
  com.ibm.ws.security.kerberos.auth
kind=noship
edition=full
