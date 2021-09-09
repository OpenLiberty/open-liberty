-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.internal.ee-9.0
singleton=true
visibility = private
-features=io.openliberty.appSecurity-4.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.pages-3.0
-bundles=\
  io.openliberty.security.oauth.internal.2.0, \
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal
-jars=\
  io.openliberty.oauth; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oauth_1.2-javadoc.zip
kind=beta
edition=core
