-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry-2.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpTelemetry-2.1
Subsystem-Name: MicroProfile Telemetry 2.1
IBM-API-Package: \
  io.opentelemetry.api.baggage;type="stable",\
  io.opentelemetry.api.baggage.propagation;type="stable",\
  io.opentelemetry.api;type="stable",\
  io.opentelemetry.api.trace;type="stable",\
  io.opentelemetry.api.trace.propagation;type="stable",\
  io.opentelemetry.api.common;type="stable",\
  io.opentelemetry.api.metrics;type="stable",\
  io.opentelemetry.api.logs;type="stable",\
  io.opentelemetry.context;type="stable",\
  io.opentelemetry.context.propagation;type="stable",\
  io.opentelemetry.extension.incubator.metrics;type="stable",\
  io.opentelemetry.sdk.trace;type="stable",\
  io.opentelemetry.sdk.trace.export;type="stable",\
  io.opentelemetry.sdk.trace.data;type="stable",\
  io.opentelemetry.sdk.trace.samplers;type="stable",\
  io.opentelemetry.sdk.metrics;type="stable",\
  io.opentelemetry.sdk.metrics.export;type="stable",\
  io.opentelemetry.sdk.metrics.data;type="stable",\
  io.opentelemetry.sdk.logs.export;type="stable",\
  io.opentelemetry.sdk.logs.data;type="stable",\
  io.opentelemetry.sdk.common;type="stable",\
  io.opentelemetry.sdk.autoconfigure.spi.logs;type="stable",\
  io.opentelemetry.sdk.autoconfigure.spi.metrics;type="stable",\
  io.opentelemetry.sdk.autoconfigure.spi.traces;type="stable",\
  io.opentelemetry.sdk.autoconfigure.spi;type="stable",\
  io.opentelemetry.semconv;type="stable",\
  io.opentelemetry.sdk.resources;type="stable",\
  io.opentelemetry.instrumentation.annotations;type="stable",\
  io.opentelemetry.exporter.logging;type="stable",\
  io.opentelemetry.exporter.otlp.logs;type="stable",\
  io.opentelemetry.exporter.otlp;type="stable"
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
-jars=io.openliberty.io.opentelemetry.2.1 ; location:="dev/api/stable/,lib/",\
  io.openliberty.microprofile.telemetry.spi; location:="dev/spi/ibm/"
-files=dev/spi/ibm/javadoc/io.openliberty.microprofile.telemetry.spi_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-7.1
