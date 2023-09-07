-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry-1.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpTelemetry-1.1
Subsystem-Name: MicroProfile Telemetry 1.1
IBM-API-Package: \
  io.opentelemetry.api.baggage;type="third-party",\
  io.opentelemetry.api.baggage.propagation;type="third-party",\
  io.opentelemetry.api;type="third-party",\
  io.opentelemetry.api.trace;type="third-party",\
  io.opentelemetry.api.events;type="third-party",\
  io.opentelemetry.api.common;type="third-party",\
  io.opentelemetry.context;type="third-party",\
  io.opentelemetry.context.propagation;type="third-party",\
  io.opentelemetry.sdk.trace;type="third-party",\
  io.opentelemetry.sdk.trace.export;type="third-party",\
  io.opentelemetry.sdk.trace.data;type="third-party",\
  io.opentelemetry.sdk.trace.samplers;type="third-party",\
  io.opentelemetry.sdk.common;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi.traces;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi;type="third-party",\
  io.opentelemetry.semconv.trace.attributes;type="third-party",\
  io.opentelemetry.semconv.resource.attributes;type="third-party",\
  io.opentelemetry.sdk.resources;type="third-party",\
  io.opentelemetry.instrumentation.annotations;type="third-party"
-features=\
  io.openliberty.jakarta.annotation-2.1, \
  io.openliberty.restfulWS-3.1, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.cdi-4.0, \
  io.openliberty.mpCompatible-6.1,\
  io.openliberty.org.eclipse.microprofile.rest.client-3.0
-bundles=\
  io.openliberty.com.squareup.okhttp,\
  io.openliberty.com.squareup.okio-jvm,\
  io.openliberty.org.jetbrains.kotlin,\
  io.openliberty.org.jetbrains.annotation,\
  io.openliberty.io.zipkin.zipkin2,\
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.1.1.internal,\
  io.openliberty.io.opentelemetry.1.29,\
  io.openliberty.microprofile.telemetry.internal.common
-jars=io.openliberty.mpTelemetry.1.1.thirdparty; location:="dev/api/third-party/,lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel 
WLP-InstantOn-Enabled: true
