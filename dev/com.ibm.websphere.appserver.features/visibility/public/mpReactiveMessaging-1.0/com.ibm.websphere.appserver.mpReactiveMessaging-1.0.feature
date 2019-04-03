-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpReactiveMessaging-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.reactive.messaging;  type="stable"
IBM-ShortName: mpReactiveMessaging-1.0
Subsystem-Name: MicroProfile Reactive Messaging 1.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.reactive.messaging-1.0, \
 com.ibm.websphere.appserver.mpReactiveStreams-1.0, \
 com.ibm.websphere.appserver.mpConfig-1.3, \
 com.ibm.websphere.appserver.cdi-2.0
-bundles=com.ibm.ws.require.java8, \
 com.ibm.ws.io.smallrye.reactive.converters, \
 com.ibm.ws.io.smallrye.reactive.messaging, \
 com.ibm.ws.io.reactivex.rxjava.2.2, \
 com.ibm.ws.org.apache.commons.lang3
kind=noship
edition=full
