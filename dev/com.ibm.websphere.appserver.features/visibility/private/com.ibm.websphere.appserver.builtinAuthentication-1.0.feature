-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.builtinAuthentication-1.0
Subsystem-Version: 1.0.0
singleton=true
-features=\
  com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1,4.0"; apiJar=false, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.ltpa-1.0,\
  com.ibm.websphere.appserver.eeCompatible-6.0; ibm.tolerates:="7.0,8.0"
-bundles=\
  com.ibm.ws.security.authentication, \
  com.ibm.ws.security.credentials.wscred, \
  com.ibm.websphere.security, \
  com.ibm.ws.security.jaas.common, \
  com.ibm.ws.security.authentication.builtin, \
  com.ibm.ws.security.mp.jwt.proxy,\
  com.ibm.ws.security.kerberos.auth
kind=ga
edition=core
