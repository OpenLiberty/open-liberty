-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.socialLogin-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: socialLogin-1.0
Subsystem-Name: Social Media Login 1.0
-features=\
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0,5.0", \
  com.ibm.websphere.appserver.jwt-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.authFilter-1.0, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  io.openliberty.socialLogin1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  com.ibm.ws.security.common.jsonwebkey, \
  io.openliberty.org.apache.commons.codec, \
  com.ibm.ws.com.google.gson.2.2.4, \
  com.ibm.json4j, \
  com.ibm.ws.org.joda.time.1.6.2, \
  io.openliberty.org.apache.commons.logging
IBM-API-Package: com.ibm.websphere.security.social; type="ibm-api"
-jars=com.ibm.websphere.appserver.api.social; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.social_1.0-javadoc.zip
kind=ga
edition=core
