-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpMetrics-5.1
visibility=public
singleton=true
IBM-API-Package: org.eclipse.microprofile.metrics.annotation;  type="stable", \
 org.eclipse.microprofile.metrics; type="stable", \
 com.ibm.websphere.endpoint; type="ibm-api", \
 com.ibm.wsspi.security.tai; type="ibm-api", \
 com.ibm.wsspi.security.token; type="ibm-api", \
 com.ibm.wsspi.security.auth.callback; type="ibm-api", \
 com.ibm.wsspi.security.common.auth.module; type="ibm-api", \
 com.ibm.websphere.security.auth.callback; type="ibm-api", \
 jakarta.annotation; type="spec", \
 jakarta.annotation.security; type="spec", \
 jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
 com.ibm.wsspi.adaptable.module, \
 com.ibm.ws.adaptable.module.structure, \
 com.ibm.wsspi.adaptable.module.adapters, \
 com.ibm.wsspi.artifact, \
 com.ibm.wsspi.artifact.factory, \
 com.ibm.wsspi.artifact.factory.contributor, \
 com.ibm.wsspi.artifact.overlay, \
 com.ibm.wsspi.artifact.equinox.module, \
 com.ibm.wsspi.http, \
 com.ibm.wsspi.http.ee8, \
 com.ibm.wsspi.anno.classsource, \
 com.ibm.wsspi.anno.info, \
 com.ibm.wsspi.anno.service, \
 com.ibm.wsspi.anno.targets, \
 com.ibm.wsspi.anno.util, \
 com.ibm.ws.anno.classsource.specification
IBM-ShortName: mpMetrics-5.1
Subsystem-Name: MicroProfile Metrics 5.1
-features=io.openliberty.restHandler.internal-1.0, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.jakarta.annotation-2.1,\
  io.openliberty.servlet.internal-6.0, \
  io.openliberty.mpCompatible-6.1, \
  io.openliberty.cdi-4.0, \
  io.openliberty.org.eclipse.microprofile.metrics-5.1, \
  com.ibm.websphere.appserver.monitor-1.0
-bundles=com.ibm.ws.microprofile.metrics.common, \
 io.openliberty.microprofile.metrics.5.1.internal, \
 io.openliberty.microprofile.metrics.5.0.private.internal, \
 io.openliberty.microprofile.metrics.5.0.public.internal, \
 io.openliberty.microprofile.metrics.5.0.monitor.internal
-jars=io.openliberty.io.smallrye.metrics; location:="lib/", \
 io.openliberty.io.micrometer; location:="lib/"
kind=ga
edition=core
WLP-InstantOn-Enabled: true
