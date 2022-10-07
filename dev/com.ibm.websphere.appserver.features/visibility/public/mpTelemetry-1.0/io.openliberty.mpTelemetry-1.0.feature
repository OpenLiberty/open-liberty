-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpTelemetry-1.0
Subsystem-Name: MicroProfile Telemetry 1.0
IBM-API-Package: \
  io.opentelemetry.api.baggage;type="stable",\
  io.opentelemetry.api;type="stable",\
  io.opentelemetry.api.trace;type="stable",\
  io.opentelemetry.context;type="stable",\
  io.opentelemetry.sdk.trace.export;type="stable",\
  io.opentelemetry.sdk.trace.data;type="stable",\
  io.opentelemetry.sdk.common;type="stable"
-features=\
  io.openliberty.jakarta.annotation-2.1, \
  io.openliberty.restfulWS-3.1, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.cdi-4.0, \
  io.openliberty.mpCompatible-6.0,\
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.org.eclipse.microprofile.telemetry-1.0
-bundles=\
  io.openliberty.com.squareup.okhttp,\
  io.openliberty.com.squareup.okio-jvm,\
  io.openliberty.org.jetbrains.kotlin,\
  io.openliberty.org.jetbrains.annotation,\
  io.openliberty.io.zipkin.zipkin2,\
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.1.0.internal,\
  io.openliberty.io.opentelemetry.internal
kind=beta
edition=core
WLP-Activation-Type: parallel 
