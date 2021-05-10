-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpReactiveStreams-1.0
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpReactiveStreams-1.0
Subsystem-Name: MicroProfile Reactive Streams 1.0
IBM-API-Package: \
  org.eclipse.microprofile.reactive.streams.operators; type="stable", \
  org.eclipse.microprofile.reactive.streams.operators.spi; type="stable", \
  org.eclipse.microprofile.reactive.streams.operators.core; type="stable", \
  org.reactivestreams; type="stable";
-features=io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0, \
  com.ibm.websphere.appserver.concurrent-1.0
-bundles=\
  com.ibm.ws.microprofile.reactive.streams.operators, \
  com.ibm.ws.io.smallrye.reactive.streams-operators, \
  com.ibm.ws.io.smallrye.reactive.converter-api, \
  com.ibm.ws.io.reactivex.rxjava.2.2
kind=ga
edition=core
WLP-Activation-Type: parallel
