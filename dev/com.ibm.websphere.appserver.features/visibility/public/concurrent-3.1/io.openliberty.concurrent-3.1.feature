-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.concurrent-3.1
visibility=public
singleton=true
IBM-ShortName: concurrent-3.1
IBM-API-Package: jakarta.enterprise.concurrent; type="spec",\
  jakarta.enterprise.concurrent.spi; type="spec", \
  jakarta.annotation; type="spec", \
  jakarta.annotation.security; type="spec", \
  jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
  com.ibm.wsspi.adaptable.module, \
  com.ibm.ws.adaptable.module.structure, \
  com.ibm.wsspi.adaptable.module.adapters, \
  com.ibm.wsspi.artifact, \
  com.ibm.wsspi.artifact.factory, \
  com.ibm.wsspi.artifact.factory.contributor, \
  com.ibm.wsspi.artifact.overlay, \
  com.ibm.wsspi.artifact.equinox.module, \
  com.ibm.wsspi.anno.classsource, \
  com.ibm.wsspi.anno.info, \
  com.ibm.wsspi.anno.service, \
  com.ibm.wsspi.anno.targets, \
  com.ibm.wsspi.anno.util, \
  com.ibm.ws.anno.classsource.specification
IBM-API-Service: jakarta.enterprise.concurrent.ContextService; id="DefaultContextService", \
  jakarta.enterprise.concurrent.ManagedExecutorService; id="DefaultManagedExecutorService", \
  jakarta.enterprise.concurrent.ManagedScheduledExecutorService; id="DefaultManagedScheduledExecutorService"
Subsystem-Name: Jakarta Concurrency 3.1
#TODO decide if autofeature should be used to avoid dependency on injection
-features=com.ibm.websphere.appserver.concurrencyPolicy-1.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  com.ibm.websphere.appserver.injection-2.0, \
  io.openliberty.jakartaeePlatform-11.0, \
  io.openliberty.jakarta.concurrency-3.1, \
  io.openliberty.jakarta.interceptor-2.2
-bundles=\
  io.openliberty.org.eclipse.microprofile.contextpropagation.1.3; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.context-propagation:microprofile-context-propagation-api:1.3", \
  com.ibm.ws.concurrent.jakarta, \
  com.ibm.ws.javaee.platform.defaultresource, \
  com.ibm.ws.resource, \
  io.openliberty.concurrent.internal,\
  io.openliberty.concurrent.internal.compat31,\
  io.openliberty.threading.internal.java21; require-java:="21"
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
