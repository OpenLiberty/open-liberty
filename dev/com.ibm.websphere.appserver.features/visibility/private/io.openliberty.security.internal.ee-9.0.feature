-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.security.internal.ee-9.0
IBM-API-Package: jakarta.servlet.annotation;  type="spec", \
 jakarta.servlet.descriptor;  type="spec", \
 jakarta.servlet.http;  type="spec", \
 jakarta.servlet;  type="spec"
visibility=private
singleton=true
-features= \
  io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0, 6.1", \
  com.ibm.websphere.appserver.builtinAuthentication-2.0, \
  io.openliberty.securityAPI.jakarta-1.0
kind=ga
edition=core
