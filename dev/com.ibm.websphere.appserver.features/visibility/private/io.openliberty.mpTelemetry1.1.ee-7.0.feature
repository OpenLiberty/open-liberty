-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry1.1.ee-7.0
singleton=true
-features=\
  com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-1.1, \
  com.ibm.websphere.appserver.mpConfig-1.3, \
  com.ibm.websphere.appserver.cdi-1.2, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.jaxrs-2.0, \
  com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=\
  com.ibm.ws.cdi.interfaces, \
  io.openliberty.microprofile.telemetry.1.1.internal, \
  io.openliberty.io.opentelemetry.1.29, \
  io.openliberty.microprofile.telemetry.internal.common
kind=ga
edition=core
WLP-Activation-Type: parallel 
