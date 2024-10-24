-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpMetrics-4.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-4.0
Subsystem-Name: MicroProfile Metrics 4.0
-features=com.ibm.websphere.appserver.restHandler-1.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.jakarta.annotation-2.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.servlet.internal-5.0, \
  io.openliberty.mpCompatible-5.0, \
  io.openliberty.cdi-3.0, \
  io.openliberty.org.eclipse.microprofile.metrics-4.0, \
  com.ibm.websphere.appserver.monitor-1.0
-bundles=com.ibm.ws.microprofile.metrics.common, \
 io.openliberty.microprofile.metrics.internal.3.0.jakarta, \
 io.openliberty.microprofile.metrics.internal.cdi.3.0.jakarta, \
 io.openliberty.microprofile.metrics.internal.private, \
 io.openliberty.microprofile.metrics.internal.public
kind=ga
edition=core
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-5.0
