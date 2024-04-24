-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwt-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-ShortName: jwt-1.0
IBM-API-Package: com.ibm.websphere.security.jwt; type="ibm-api", \
  com.ibm.wsspi.security.tai; type="ibm-api", \
  com.ibm.wsspi.security.token; type="ibm-api", \
  com.ibm.wsspi.security.auth.callback; type="ibm-api", \
  com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
  com.ibm.websphere.security.auth.callback; type="ibm-api"
Subsystem-Name: JSON Web Token 1.0
-features=io.openliberty.servlet.internal-3.0; ibm.tolerates:="3.1,4.0,5.0,6.0,6.1", \
  io.openliberty.webBundleSecurity.internal-1.0, \
  io.openliberty.jwt1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-bundles=\
  com.ibm.ws.org.apache.httpcomponents, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.ws.security.common.jsonwebkey, \
  io.openliberty.org.apache.commons.codec, \
  com.ibm.ws.org.jose4j, \
  io.openliberty.com.google.gson, \
  com.ibm.json4j
-jars=\
  com.ibm.websphere.appserver.api.jwt; location:=dev/api/ibm/, \
  io.openliberty.jwt; location:=dev/api/ibm/
-files=\
  dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jwt_1.1-javadoc.zip, \
  dev/api/ibm/javadoc/io.openliberty.jwt_1.1-javadoc.zip
kind=ga
edition=core
WLP-InstantOn-Enabled: true
