-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: concurrentRX-1.0
Subsystem-Name: EE Concurrency support for JAX-RS managed completable futures
symbolicName=com.ibm.websphere.appserver.concurrentRX-1.0
visibility=public
-features=\
  com.ibm.websphere.appserver.concurrent-1.0, \
  com.ibm.websphere.appserver.jaxrs-2.1
-bundles=\
  com.ibm.ws.concurrent.rx
# Do not release this feature until ManagedCompletableFuture has been properly rebased for Java 9 (see TODO comments in that file)
kind=noship
edition=full
