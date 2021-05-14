-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpMetrics-2.3
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-2.3
Subsystem-Name: MicroProfile Metrics 2.3
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.mpConfig-1.4; ibm.tolerates:="1.3", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.javax.annotation-1.3; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.servlet-4.0; ibm.tolerates:="3.1", \
  com.ibm.websphere.appserver.cdi-2.0; ibm.tolerates:="1.2", \
  com.ibm.websphere.appserver.monitor-1.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.metrics-2.3
-bundles=com.ibm.ws.microprofile.metrics.common, \
 com.ibm.ws.microprofile.metrics.2.3, \
 com.ibm.ws.microprofile.metrics.cdi.2.3, \
 com.ibm.ws.microprofile.metrics.private, \
 com.ibm.ws.microprofile.metrics.public
kind=ga
edition=core
