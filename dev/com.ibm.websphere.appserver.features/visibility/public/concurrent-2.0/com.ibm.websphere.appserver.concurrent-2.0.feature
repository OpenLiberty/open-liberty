-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.concurrent-2.0
visibility=public
singleton=true
IBM-ShortName: concurrent-2.0
IBM-API-Package: jakarta.enterprise.concurrent; type="spec"
IBM-API-Service: jakarta.enterprise.concurrent.ContextService; id="DefaultContextService", \
 jakarta.enterprise.concurrent.ManagedExecutorService; id="DefaultManagedExecutorService", \
 jakarta.enterprise.concurrent.ManagedScheduledExecutorService; id="DefaultManagedScheduledExecutorService"
Subsystem-Name: Jakarta EE Concurrency 2.0
-features=\
 com.ibm.websphere.appserver.jakartaeePlatform-9.0, \
 com.ibm.websphere.appserver.appLifecycle-1.0, \
 com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.contextService-1.0, \
 com.ibm.websphere.appserver.jakarta.concurrency-2.0, \
 com.ibm.websphere.appserver.org.eclipse.microprofile.contextpropagation-1.0; ibm.tolerates:="1.1"
-bundles=\
 com.ibm.websphere.javaee.concurrent.1.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.enterprise.concurrent:javax.enterprise.concurrent-api:1.0", \
 com.ibm.ws.concurrent, \
 com.ibm.ws.javaee.platform.defaultresource, \
 com.ibm.ws.resource
kind=noship
edition=core
WLP-Activation-Type: parallel
