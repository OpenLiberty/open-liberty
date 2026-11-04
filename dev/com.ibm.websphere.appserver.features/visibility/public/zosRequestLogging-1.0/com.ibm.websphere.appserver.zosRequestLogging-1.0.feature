-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.zosRequestLogging-1.0
visibility=public
IBM-API-Package: com.ibm.websphere.zos.request.logging;type="ibm-api"
IBM-ShortName: zosRequestLogging-1.0
Subsystem-Name: z/OS Request Logging 1.0
-bundles=com.ibm.ws.zos.request.logging, \
 com.ibm.websphere.zos.request.logging, \
 com.ibm.ws.resource, \
 com.ibm.ws.zos.request.logging.data
-features=\
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.servlet.api-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.requestProbeHttp-1.0, \
  com.ibm.websphere.appserver.requestProbeServlet-1.0, \
  com.ibm.websphere.appserver.requestProbeZosWlm-1.0
-jars=com.ibm.websphere.appserver.api.zosRequestLogging; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.zosRequestLogging_1.0-javadoc.zip
kind=ga
edition=zos
