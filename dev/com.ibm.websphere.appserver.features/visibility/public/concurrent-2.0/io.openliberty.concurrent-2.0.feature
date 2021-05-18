-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.concurrent-2.0
visibility=public
singleton=true
IBM-ShortName: concurrent-2.0
IBM-API-Package: jakarta.enterprise.concurrent; type="spec"
IBM-API-Service: jakarta.enterprise.concurrent.ContextService; id="DefaultContextService", \
  jakarta.enterprise.concurrent.ManagedExecutorService; id="DefaultManagedExecutorService", \
  jakarta.enterprise.concurrent.ManagedScheduledExecutorService; id="DefaultManagedScheduledExecutorService"
Subsystem-Name: Jakarta Concurrency 2.0
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  io.openliberty.jakartaeePlatform-9.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  io.openliberty.jakarta.concurrency-2.0, \
  com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.contextpropagation-1.0; ibm.tolerates:="1.2"
-bundles=\
  com.ibm.ws.concurrent.jakarta, \
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.ws.resource
kind=beta
edition=core
WLP-Activation-Type: parallel
