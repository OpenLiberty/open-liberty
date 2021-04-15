-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.openid-2.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: openid-2.0
Subsystem-Name: OpenID 2.0
-features=com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.authFilter-1.0
-bundles=com.ibm.ws.org.openid4java.0.9.7, \
  com.ibm.ws.org.apache.xml.resolver.1.2, \
  com.ibm.ws.security.openid.2.0, \
  io.openliberty.org.apache.commons.codec, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.ws.com.google.guice.2.0, \
  com.ibm.ws.org.cyberneko.html.1.9.18
kind=ga
edition=core
