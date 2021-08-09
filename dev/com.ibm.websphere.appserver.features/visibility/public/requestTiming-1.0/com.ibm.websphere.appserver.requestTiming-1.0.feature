-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.requestTiming-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-API-Package: com.ibm.websphere.interrupt;type="internal"
IBM-ShortName: requestTiming-1.0
Subsystem-Name: Request Timing 1.0
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.requestProbeServlet-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.requestProbeJDBC-1.0
-bundles=com.ibm.ws.request.timing, \
 com.ibm.websphere.interrupt, \
 com.ibm.ws.request.interrupt
kind=ga
edition=core
