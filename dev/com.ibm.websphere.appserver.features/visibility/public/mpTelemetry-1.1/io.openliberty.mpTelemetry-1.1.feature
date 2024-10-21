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
IBM-SPI-Package: io.openliberty.microprofile.telemetry.spi
-features=\
  io.openliberty.mpTelemetry1.1.ee-10.0; ibm.tolerates:= "9.0, 8.0, 7.0"
-bundles=\
  io.openliberty.com.squareup.okhttp,\
  io.openliberty.com.squareup.okio-jvm,\
  io.openliberty.org.jetbrains.kotlin,\
  io.openliberty.org.jetbrains.annotation,\
  io.openliberty.io.zipkin.zipkin2.1.0
-jars=io.openliberty.mpTelemetry.1.1.thirdparty; location:="dev/api/third-party/,lib/",\
  io.openliberty.microprofile.telemetry.spi; location:="lib/"
# commented out until the SPI is moved to GA and the bundle is moved to the dev/spi/ibm direcotry
#-files=dev/spi/ibm/javadoc/io.openliberty.microprofile.telemetry.spi_1.0-javadoc.zip
# add dev/spi/ibm/, when the SPI is moved to GA
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-6.1
