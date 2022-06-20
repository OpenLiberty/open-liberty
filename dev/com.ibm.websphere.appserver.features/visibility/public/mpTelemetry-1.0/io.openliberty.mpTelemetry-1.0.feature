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
  io.opentelemetry.extension.annotations;type="stable",\
  io.opentelemetry.api.trace;type="stable",\
  io.opentelemetry.api.baggage;type="stable",\
  io.opentelemetry.context;type="stable"
-features=\
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.jakarta.annotation-2.0, \
  io.openliberty.restfulWS-3.0, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.cdi-3.0, \
  io.openliberty.mpCompatible-5.0,\
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.grpcClient1.0.internal.ee-9.0,\
  io.openliberty.grpcClient-1.0,\
  io.openliberty.io.netty,\
  com.ibm.websphere.appserver.channelfw-1.0
-bundles=\
  io.openliberty.com.fasterxml.jackson.jr, \
  io.openliberty.com.fasterxml.jackson, \
  com.ibm.ws.transport.http,\
  io.openliberty.accesslists.internal, \
  com.ibm.ws.managedobject,\
  io.openliberty.grpc.1.0.internal.common.jakarta,\
  io.openliberty.grpc.1.0.internal.shaded.jakarta, \
  com.ibm.ws.com.google.guava,\
  io.openliberty.microprofile.telemetry.1.0.internal.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel 
