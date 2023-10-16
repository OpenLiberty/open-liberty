-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpTelemetry1.1.ee-7.0
singleton=true
-features=\
  com.ibm.websphere.appserver.mpRestClient-1.0, \
  com.ibm.websphere.appserver.mpConfig-1.2, \
  com.ibm.websphere.appserver.cdi-1.2, \
  io.openliberty.mpCompatible-0.0, \ 
  com.ibm.websphere.appserver.eeCompatible-7.0
-bundles=\
  com.ibm.ws.cdi.interfaces, \
  io.openliberty.microprofile.telemetry.1.1.internal, \
  io.openliberty.io.opentelemetry.1.29, \
  io.openliberty.microprofile.telemetry.internal.common
-jars=io.openliberty.mpTelemetry.1.1.thirdparty; location:="dev/api/third-party/,lib/"
kind=beta
edition=core
WLP-Activation-Type: parallel 
WLP-InstantOn-Enabled: true