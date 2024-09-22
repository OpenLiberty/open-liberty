-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry2.0.ee-8.0
singleton=true
-features=\
  com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-2.0,\
  com.ibm.websphere.appserver.mpConfig-2.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  io.openliberty.mpCompatible-4.0, \
  com.ibm.websphere.appserver.jaxrs-2.1, \
  com.ibm.websphere.appserver.eeCompatible-8.0
-bundles=\
  com.ibm.ws.collector, \
  com.ibm.ws.cdi.interfaces, \
  io.openliberty.microprofile.telemetry.2.0.internal, \
  io.openliberty.microprofile.telemetry.2.0.logging.internal, \
  io.openliberty.io.opentelemetry.2.0, \
  io.openliberty.microprofile.telemetry.internal.common
kind=ga
edition=core
WLP-Activation-Type: parallel 
