-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.security.internal.common-1.0
WLP-DisableAllFeatures-OnConflict: false
-features=com.ibm.websphere.appserver.basicRegistry-1.0, \
  com.ibm.websphere.appserver.builtinAuthorization-1.0, \
  com.ibm.websphere.appserver.ssl-1.0, \
  com.ibm.websphere.appserver.ltpa-1.0
-bundles=com.ibm.websphere.security.impl, \
 com.ibm.ws.management.security, \
 com.ibm.ws.security.quickstart
-jars= \
 com.ibm.websphere.appserver.api.security.spnego; location:=dev/api/ibm/
-files= \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.security.spnego_1.1-javadoc.zip
kind=ga
edition=core
