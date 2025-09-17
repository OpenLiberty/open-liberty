-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry2.1.ee-10.0
singleton=true
-features=\
  io.openliberty.restfulWS-3.1, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.cdi-4.0, \
  io.openliberty.mpCompatible-7.1, \
  io.openliberty.org.eclipse.microprofile.rest.client-4.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0
-bundles=\
  com.ibm.ws.collector.jakarta, \
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.2.0.internal.jakarta,\
  io.openliberty.microprofile.telemetry.2.1.logging.internal.jakarta, \
  io.openliberty.io.opentelemetry.internal.2.1.jakarta,\
  io.openliberty.microprofile.telemetry.internal.common.jakarta,\
  io.openliberty.microprofile.telemetry.logging.internal.common.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel
