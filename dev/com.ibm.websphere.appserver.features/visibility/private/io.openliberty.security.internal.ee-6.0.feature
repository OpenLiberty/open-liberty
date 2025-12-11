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
  com.ibm.websphere.appserver.builtinAuthentication-1.0, \
  io.openliberty.securityAPI.javaee-1.0
kind=ga
edition=core
