-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.security.internal.ee-9.0
visibility=private
singleton=true
-features= \
  io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0", \
  com.ibm.websphere.appserver.builtinAuthentication-2.0, \
  io.openliberty.securityAPI.jakarta-1.0
kind=ga
edition=core
