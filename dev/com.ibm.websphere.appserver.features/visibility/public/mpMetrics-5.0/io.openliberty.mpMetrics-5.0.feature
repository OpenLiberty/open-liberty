-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpMetrics-5.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-5.0
Subsystem-Name: MicroProfile Metrics 5.0
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.jakarta.annotation-2.1; ibm.tolerates:="2.0", \
  com.ibm.websphere.appserver.servlet-6.0; ibm.tolerates:="5.0", \
  io.openliberty.mpCompatible-6.0, \
  io.openliberty.cdi-4.0; ibm.tolerates:="3.0", \
  io.openliberty.org.eclipse.microprofile.metrics-5.0, \
  com.ibm.websphere.appserver.anno-2.0, \
  com.ibm.websphere.appserver.monitor-1.0
-bundles=com.ibm.ws.microprofile.metrics.common, \
 io.openliberty.microprofile.metrics.internal.3.0.jakarta, \
 io.openliberty.microprofile.metrics.internal.cdi.3.0.jakarta, \
 io.openliberty.microprofile.metrics.internal.private, \
 io.openliberty.microprofile.metrics.internal.public
kind=noship
edition=full