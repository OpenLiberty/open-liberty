-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry2.1.ee-11.0
singleton=true
-features=\
  io.openliberty.restfulWS-4.0, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.cdi-4.1, \
  io.openliberty.mpCompatible-7.1,\
  io.openliberty.org.eclipse.microprofile.rest.client-4.0,\
  com.ibm.websphere.appserver.eeCompatible-11.0,\
  io.openliberty.noShip-1.0
-bundles=\
  com.ibm.ws.collector.jakarta, \
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.2.1.internal.jakarta,\
  io.openliberty.microprofile.telemetry.2.1.logging.internal.jakarta, \
  io.openliberty.io.opentelemetry.2.1.jakarta,\
  io.openliberty.microprofile.telemetry.internal.common.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel 
