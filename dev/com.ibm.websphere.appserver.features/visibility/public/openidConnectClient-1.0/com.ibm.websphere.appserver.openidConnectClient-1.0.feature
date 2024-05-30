-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.openidConnectClient-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.security.openidconnect; type="ibm-api"
IBM-ShortName: openidConnectClient-1.0
Subsystem-Name: OpenID Connect Client 1.0

# due to ConvergedClientConfig having jwt deps, oidc client now has jwt deps.
-features=io.openliberty.openidConnectClient1.0.internal.ee-6.0; ibm.tolerates:="9.0, 10.0, 11.0", \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.oauth-2.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1"
-bundles=\
  com.ibm.ws.org.joda.time.1.6.2, \
  com.ibm.json4j, \
  io.openliberty.org.apache.commons.codec, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.ws.security.openidconnect.common, \
  com.ibm.ws.security.common.jsonwebkey, \
  io.openliberty.com.google.gson, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.org.json.simple.1.1.1, \
  com.ibm.ws.org.apache.commons.lang3
-jars=\
  com.ibm.websphere.appserver.api.oidc; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.oidc_1.0-javadoc.zip
kind=ga
edition=core
WLP-InstantOn-Enabled: true
