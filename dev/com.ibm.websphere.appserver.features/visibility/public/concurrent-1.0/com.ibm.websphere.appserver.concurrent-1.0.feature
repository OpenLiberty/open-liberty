-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent-1.0
visibility=public
IBM-API-Package: javax.enterprise.concurrent; type="spec"
IBM-ShortName: concurrent-1.0
IBM-API-Service: javax.enterprise.concurrent.ContextService; id="DefaultContextService", \
 javax.enterprise.concurrent.ManagedExecutorService; id="DefaultManagedExecutorService", \
 javax.enterprise.concurrent.ManagedScheduledExecutorService; id="DefaultManagedScheduledExecutorService"
Subsystem-Name: Concurrency Utilities for Java EE 1.0
-features=com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.appLifecycle-1.0, \
 com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
 com.ibm.websphere.appserver.concurrent.mp-0.0.0.noImpl; ibm.tolerates:=1.0, \
 com.ibm.websphere.appserver.contextService-1.0
-bundles=com.ibm.ws.javaee.platform.defaultresource, \
 com.ibm.websphere.javaee.concurrent.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.0", \
 com.ibm.ws.resource, \
 com.ibm.ws.concurrent
kind=ga
edition=core
WLP-Activation-Type: parallel
