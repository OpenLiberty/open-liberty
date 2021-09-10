-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.concurrent-3.0
visibility=public
singleton=true
IBM-ShortName: concurrent-3.0
IBM-API-Package: jakarta.enterprise.concurrent; type="spec"
IBM-API-Service: jakarta.enterprise.concurrent.ContextService; id="DefaultContextService", \
  jakarta.enterprise.concurrent.ManagedExecutorService; id="DefaultManagedExecutorService", \
  jakarta.enterprise.concurrent.ManagedScheduledExecutorService; id="DefaultManagedScheduledExecutorService"
Subsystem-Name: Jakarta Concurrency 3.0
#TODO switch to io.openliberty.jakarta.concurrency-3.0 spec, once available
#TODO switch to eeCompatible-10.0 at same time as other EE 10 features, once they exist
#TODO switch to interceptor-vNext, or remove if spec doesn't end up with @Async
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakartaeePlatform-10.0, \
  io.openliberty.jakarta.concurrency-2.0, \
  io.openliberty.jakarta.interceptor-2.0, \
  io.openliberty.org.eclipse.microprofile.contextpropagation-1.3
-bundles=\
  com.ibm.ws.concurrent.jakarta, \
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.ws.resource
kind=noship
edition=full
WLP-Activation-Type: parallel
