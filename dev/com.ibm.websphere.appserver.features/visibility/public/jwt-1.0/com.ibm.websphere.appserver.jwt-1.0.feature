-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jwt-1.0
visibility=public
IBM-ShortName: jwt-1.0
IBM-API-Package: com.ibm.websphere.security.jwt; type="ibm-api"
Subsystem-Name: JSON Web Token 1.0
-features=com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.wsspi.appserver.webBundleSecurity-1.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1, 4.0"
-jars=com.ibm.websphere.appserver.api.jwt; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.jwt_1.1-javadoc.zip
-bundles=com.ibm.ws.security.jwt, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.apache.commons.logging.1.0.3, \
  com.ibm.ws.security.common, \
  com.ibm.ws.security.common.jsonwebkey, \
  com.ibm.ws.org.apache.commons.codec, \
  com.ibm.ws.org.jose4j, \
  com.ibm.ws.com.google.gson.2.2.4, \
  com.ibm.ws.org.slf4j.api.1.7.7, \
  com.ibm.ws.org.slf4j.jdk14.1.7.7, \
  com.ibm.json4j
kind=ga
edition=core
