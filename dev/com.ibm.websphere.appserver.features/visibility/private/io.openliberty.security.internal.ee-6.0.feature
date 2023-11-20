-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.security.internal.ee-6.0
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: javax.servlet.annotation;  type="spec", \
 javax.servlet.descriptor;  type="spec", \
 javax.servlet.http;  type="spec", \
 javax.servlet;  type="spec"
visibility=private
singleton=true
-features= \
  io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.builtinAuthentication-1.0, \
  io.openliberty.securityAPI.javaee-1.0
kind=ga
edition=core
