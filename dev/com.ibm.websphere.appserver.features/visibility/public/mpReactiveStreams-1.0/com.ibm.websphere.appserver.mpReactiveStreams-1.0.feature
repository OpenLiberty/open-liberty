-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpReactiveStreams-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mpReactiveStreams-1.0
Subsystem-Name: MicroProfile Reactive Streams 1.0
IBM-API-Package: \
  org.eclipse.microprofile.reactive.streams; type="stable", \
  org.reactivestreams; type="stable"
-features=\
  com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0
-bundles=\
  com.ibm.ws.microprofile.reactive.streams.operators; apiJar=false; location:="lib/"
kind=noship
edition=full