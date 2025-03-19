-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry-2.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpTelemetry-2.1
Subsystem-Name: MicroProfile Telemetry 2.1
IBM-API-Package: \
  io.opentelemetry.api.baggage;type="third-party",\
  io.opentelemetry.api.baggage.propagation;type="third-party",\
  io.opentelemetry.api;type="third-party",\
  io.opentelemetry.api.trace;type="third-party",\
  io.opentelemetry.api.trace.propagation;type="third-party",\
  io.opentelemetry.api.common;type="third-party",\
  io.opentelemetry.api.metrics;type="third-party",\
  io.opentelemetry.api.logs;type="third-party",\
  io.opentelemetry.context;type="third-party",\
  io.opentelemetry.context.propagation;type="third-party",\
  io.opentelemetry.extension.incubator.metrics;type="third-party",\
  io.opentelemetry.sdk.trace;type="third-party",\
  io.opentelemetry.sdk.trace.export;type="third-party",\
  io.opentelemetry.sdk.trace.data;type="third-party",\
  io.opentelemetry.sdk.trace.samplers;type="third-party",\
  io.opentelemetry.sdk.metrics;type="third-party",\
  io.opentelemetry.sdk.metrics.export;type="third-party",\
  io.opentelemetry.sdk.metrics.data;type="third-party",\
  io.opentelemetry.sdk.logs.export;type="third-party",\
  io.opentelemetry.sdk.logs.data;type="third-party",\
  io.opentelemetry.sdk.common;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi.logs;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi.metrics;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi.traces;type="third-party",\
  io.opentelemetry.sdk.autoconfigure.spi;type="third-party",\
  io.opentelemetry.semconv;type="third-party",\
  io.opentelemetry.sdk.resources;type="third-party",\
  io.opentelemetry.instrumentation.annotations;type="third-party",\
  io.opentelemetry.exporter.logging;type="third-party",\
  io.opentelemetry.exporter.otlp.logs;type="third-party",\
  io.opentelemetry.exporter.otlp;type="third-party"
IBM-SPI-Package: io.openliberty.microprofile.telemetry.spi
-features=\
  io.openliberty.mpTelemetry2.1.ee-10.0; ibm.tolerates:= "11.0, 9.0, 8.0, 7.0",\
  com.ibm.websphere.appserver.monitor-1.0
-bundles=\
  io.openliberty.com.squareup.okhttp,\
  io.openliberty.com.squareup.okio-jvm,\
  io.openliberty.org.jetbrains.kotlin,\
  io.openliberty.org.jetbrains.annotation,\
  io.openliberty.io.zipkin.zipkin2.2.0,\
  io.openliberty.microprofile.telemetry.monitor.internal
-jars=io.openliberty.mpTelemetry.2.1.thirdparty; location:="dev/api/third-party/,lib/",\
  io.openliberty.microprofile.telemetry.spi; location:="dev/spi/ibm/"
-files=dev/spi/ibm/javadoc/io.openliberty.microprofile.telemetry.spi_1.0-javadoc.zip
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-7.0
