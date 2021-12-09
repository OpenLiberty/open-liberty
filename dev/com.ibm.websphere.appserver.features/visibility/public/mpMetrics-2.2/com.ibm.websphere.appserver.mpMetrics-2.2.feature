-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpMetrics-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-2.2
Subsystem-Name: MicroProfile Metrics 2.2
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  com.ibm.websphere.appserver.mpConfig-1.3; ibm.tolerates:="1.4", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.javax.annotation-1.2; ibm.tolerates:="1.3", \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="4.0", \
  com.ibm.websphere.appserver.org.eclipse.microprofile.metrics-2.2, \
  com.ibm.websphere.appserver.cdi-1.2; ibm.tolerates:="2.0"
-bundles=com.ibm.ws.microprofile.metrics.common, \
 com.ibm.ws.microprofile.metrics.2.0, \
 com.ibm.ws.microprofile.metrics.private, \
 com.ibm.ws.microprofile.metrics.public
kind=ga
edition=core
