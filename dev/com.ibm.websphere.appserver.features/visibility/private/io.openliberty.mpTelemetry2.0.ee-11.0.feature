-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry2.0.ee-11.0
singleton=true
-features=\
  io.openliberty.restfulWS-4.0, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.cdi-4.1, \
  io.openliberty.mpCompatible-7.0, \
  io.openliberty.org.eclipse.microprofile.rest.client-4.0,\
  com.ibm.websphere.appserver.eeCompatible-11.0
-bundles=\
  com.ibm.ws.collector.jakarta, \
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.2.0.internal.jakarta,\
  io.openliberty.microprofile.telemetry.2.0.logging.internal.jakarta, \
  io.openliberty.io.opentelemetry.2.0.jakarta,\
  io.openliberty.microprofile.telemetry.internal.common.jakarta,\
  io.openliberty.microprofile.telemetry.logging.internal.common.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel 
