-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.jakarta
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.oauth-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.servlet-5.0))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.appSecurity-4.0, \
  io.openliberty.pages-3.0
-bundles=\
  io.openliberty.security.oauth.internal.2.0, \
  io.openliberty.security.jwt.internal, \
  io.openliberty.security.common.internal, \
  io.openliberty.oauth; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oauth_1.2-javadoc.zip
kind=noship
edition=full
