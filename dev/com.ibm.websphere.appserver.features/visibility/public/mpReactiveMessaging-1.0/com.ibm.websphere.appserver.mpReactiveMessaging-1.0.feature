-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpReactiveMessaging-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.reactive.messaging;  type="stable", \
  org.eclipse.microprofile.reactive.messaging.spi; type="stable", \
  com.ibm.ws.kafka.security; type="internal"
IBM-ShortName: mpReactiveMessaging-1.0
Subsystem-Name: MicroProfile Reactive Messaging 1.0
-features=com.ibm.websphere.appserver.mpConfig-1.3; ibm.tolerates:="1.4", \
  com.ibm.websphere.appserver.mpReactiveStreams-1.0, \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.messaging-1.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.concurrent-1.0
-bundles=com.ibm.ws.io.smallrye.reactive.messaging-provider, \
 com.ibm.ws.io.reactivex.rxjava.2.2, \
 com.ibm.ws.org.apache.commons.lang3, \
 com.ibm.ws.microprofile.reactive.messaging.kafka, \
 com.ibm.ws.microprofile.reactive.messaging.kafka.adapter, \
 com.ibm.ws.microprofile.reactive.messaging.kafka.adapter.impl
kind=ga
edition=core
WLP-Activation-Type: parallel
