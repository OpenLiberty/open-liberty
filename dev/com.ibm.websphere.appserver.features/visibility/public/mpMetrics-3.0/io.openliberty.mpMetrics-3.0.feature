-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpMetrics-3.0
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable"
IBM-ShortName: mpMetrics-3.0
Subsystem-Name: MicroProfile Metrics 3.0
-features=io.openliberty.org.eclipse.microprofile.metrics-3.0, \
 com.ibm.websphere.appserver.cdi-2.0,\
 com.ibm.websphere.appserver.javax.annotation-1.3, \
 com.ibm.websphere.appserver.restHandler-1.0, \
 com.ibm.websphere.appserver.monitor-1.0, \
 com.ibm.websphere.appserver.servlet-4.0,\
 io.openliberty.mpConfig-2.0
-bundles=com.ibm.ws.microprofile.metrics.common, \
 io.openliberty.microprofile.metrics.internal.3.0, \
 io.openliberty.microprofile.metrics.internal.cdi.3.0, \
 io.openliberty.microprofile.metrics.internal.private, \
 io.openliberty.microprofile.metrics.internal.public
kind=beta
edition=core