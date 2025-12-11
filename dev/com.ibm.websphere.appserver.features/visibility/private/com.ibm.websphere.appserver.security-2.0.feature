-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.security-2.0
singleton=true
IBM-API-Package: com.ibm.wsspi.security.tai; type="ibm-api", \
 com.ibm.wsspi.security.token; type="ibm-api", \
 com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api"
-features= \
  io.openliberty.servlet.api-5.0; apiJar=false; ibm.tolerates:="6.0,6.1", \
  io.openliberty.security.internal.common-1.0, \
  io.openliberty.security.internal.ee-9.0
kind=ga
edition=core
