-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.security-1.0
singleton=true
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: com.ibm.wsspi.security.tai; type="ibm-api", \
 com.ibm.wsspi.security.token; type="ibm-api", \
 com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api"
-features= \
  io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
  io.openliberty.security.internal.common-1.0, \
  io.openliberty.security.internal.ee-6.0
kind=ga
edition=core
