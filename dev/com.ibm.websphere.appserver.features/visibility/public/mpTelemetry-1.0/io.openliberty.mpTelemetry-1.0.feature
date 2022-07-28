-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry-1.0
visibility=public
singleton=true
jakartaeeMe: true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpTelemetry-1.0
Subsystem-Name: MicroProfile Telemetry 1.0
IBM-API-Package: \
  io.opentelemetry.api.baggage;type="stable",\
  io.opentelemetry.api;type="stable",\
  io.opentelemetry.api.trace;type="stable"
-features=\
  io.openliberty.jakarta.annotation-2.0, \
  io.openliberty.restfulWS-3.0, \
  io.openliberty.mpRestClient-3.0,\
  io.openliberty.mpConfig-3.0, \
  io.openliberty.cdi-3.0, \
  io.openliberty.mpCompatible-5.0,\
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.org.eclipse.microprofile.telemetry-1.0
-bundles=\
  io.openliberty.com.fasterxml.jackson.jr, \
  io.openliberty.com.fasterxml.jackson, \
  com.ibm.ws.transport.http,\
  io.openliberty.accesslists.internal, \
  com.ibm.ws.managedobject,\
  io.openliberty.com.squareup.okhttp,\
  io.openliberty.com.squareup.okio-jvm,\
  io.openliberty.org.jetbrains.kotlin,\
  io.openliberty.org.jetbrains.annotation,\
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.1.0.internal,\
  io.openliberty.io.opentelemetry.internal,\
  io.openliberty.io.opentelemetry; location:="dev/api/stable/,lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel 
