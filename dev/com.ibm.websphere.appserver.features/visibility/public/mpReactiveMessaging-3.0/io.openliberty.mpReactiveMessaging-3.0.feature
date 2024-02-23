-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpReactiveMessaging-3.0
WLP-DisableAllFeatures-OnConflict: true
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.reactive.messaging; type="stable", \
  org.eclipse.microprofile.reactive.messaging.spi; type="stable", \
  com.ibm.ws.kafka.security; type="internal"
IBM-ShortName: mpReactiveMessaging-3.0
Subsystem-Name: MicroProfile Reactive Messaging 3.0
-features=io.openliberty.mpConfig-3.0; ibm.tolerates:="3.1", \
  io.openliberty.mpReactiveStreams-3.0, \
  io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1", \
  io.openliberty.org.eclipse.microprofile.reactive.messaging-3.0, \
  io.openliberty.cdi-3.0; ibm.tolerates:="4.0",  \
  io.openliberty.org.eclipse.microprofile.metrics-4.0; ibm.tolerates:="5.0,5.1"
-bundles=io.openliberty.io.smallrye.reactive.messaging-provider4, \
 io.openliberty.io.smallrye.reactive.converter-api3, \
 com.ibm.ws.microprofile.reactive.messaging.kafka.jakarta, \
 com.ibm.ws.microprofile.reactive.messaging.kafka.adapter, \
 com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl, \
 io.openliberty.microprofile.reactive.messaging.internal,\
 io.openliberty.microprofile.reactive.messaging.3.0.internal
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
