-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpReactiveStreams-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: org.eclipse.microprofile.reactive.streams;  type="stable", \
IBM-ShortName: mpReactiveStreams-1.0
Subsystem-Name: MicroProfile Reactive Streams 1.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.streams.operators-1.0, \
 com.ibm.websphere.appserver.concurrent-1.0
-bundles=com.ibm.ws.require.java8, \
 com.ibm.ws.org.reactivestreams.interfaces; apiJar=true; location:="lib/"
kind=beta
edition=core
