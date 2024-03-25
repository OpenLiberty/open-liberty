-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpReactiveStreams-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpReactiveStreams-3.0
Subsystem-Name: MicroProfile Reactive Streams 3.0
IBM-API-Package: \
  org.eclipse.microprofile.reactive.streams.operators; type="stable", \
  org.eclipse.microprofile.reactive.streams.operators.spi; type="stable", \
  org.eclipse.microprofile.reactive.streams.operators.core; type="stable", \
  org.reactivestreams; type="stable";
-features=io.openliberty.mpCompatible-5.0; ibm.tolerates:="6.0,6.1", \
  io.openliberty.jakarta.cdi-3.0; ibm.tolerates:="4.0", \
  io.openliberty.org.eclipse.microprofile.reactive.streams.operators-3.0
-bundles=\
  io.openliberty.microprofile.reactive.streams.operators30.internal, \
  io.openliberty.io.smallrye.reactive.streams-operators-jakarta, \
  io.openliberty.io.smallrye.reactive.mutiny,\
  io.openliberty.io.smallrye.common2, \
  io.openliberty.org.jboss.logging35
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
