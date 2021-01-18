-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName = io.openliberty.oauth2.0.javaee
visibility = private
IBM-Provision-Capability:\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(osgi.identity=com.ibm.websphere.appserver.oauth-2.0))",\
  osgi.identity; filter:="(&(type=osgi.subsystem.feature)(|(osgi.identity=com.ibm.websphere.appserver.servlet-3.0)(osgi.identity=com.ibm.websphere.appserver.servlet-3.1)(osgi.identity=com.ibm.websphere.appserver.servlet-4.0)))"
IBM-Install-Policy: when-satisfied
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.jsp-2.2; ibm.tolerates:=2.3
-bundles=\
  com.ibm.ws.security.oauth.2.0, \
  com.ibm.ws.security.jwt, \
  com.ibm.ws.security.common, \
  com.ibm.websphere.appserver.api.oauth; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oauth_1.2-javadoc.zip
kind=ga
edition=core
