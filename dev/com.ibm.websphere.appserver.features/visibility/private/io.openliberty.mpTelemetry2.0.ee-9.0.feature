-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry2.0.ee-9.0
singleton=true
-features=\
  io.openliberty.mpConfig-3.0, \
  io.openliberty.cdi-3.0, \
  io.openliberty.mpCompatible-5.0,\
  io.openliberty.restfulWS-3.0, \
  io.openliberty.org.eclipse.microprofile.rest.client-3.0,\
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.2.0.internal.jakarta,\
  io.openliberty.io.opentelemetry.2.0.jakarta,\
  io.openliberty.microprofile.telemetry.internal.common.jakarta
kind=ga
edition=core
WLP-Activation-Type: parallel 
