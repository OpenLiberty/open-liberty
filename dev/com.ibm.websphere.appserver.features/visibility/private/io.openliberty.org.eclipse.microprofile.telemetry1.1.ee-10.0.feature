-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry1.1.ee-10.0
singleton=true
-features=\
  io.openliberty.restfulWS-3.1, \
  io.openliberty.mpConfig-3.0, \
  io.openliberty.cdi-4.0, \
  io.openliberty.mpCompatible-6.0,\
  io.openliberty.org.eclipse.microprofile.rest.client-3.0
-bundles=\
  com.ibm.ws.cdi.interfaces.jakarta, \
  io.openliberty.microprofile.telemetry.1.1.internal.jakarta,\
  io.openliberty.io.opentelemetry.1.29.jakarta,\
  io.openliberty.microprofile.telemetry.internal.common.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel 
WLP-InstantOn-Enabled: true