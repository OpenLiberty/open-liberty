-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batch-2.2
singleton=true
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  jakarta.batch.api; type="spec", \
  jakarta.batch.api.chunk; type="spec", \
  jakarta.batch.api.chunk.listener; type="spec", \
  jakarta.batch.api.listener; type="spec", \
  jakarta.batch.api.partition; type="spec", \
  jakarta.batch.operations; type="spec", \
  jakarta.batch.runtime; type="spec", \
  jakarta.batch.runtime.context; type="spec", \
  jakarta.inject;  type="spec", \
  com.ibm.websphere.persistence.mbean; type="ibm-api"
IBM-ShortName: batch-2.2
Subsystem-Name: Jakarta Batch 2.2
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.servlet-6.2, \
  io.openliberty.persistence-4.0, \
  io.openliberty.jakarta.annotation-3.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  io.openliberty.persistenceService-2.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.jakarta.cdi-5.0, \
  com.ibm.websphere.appserver.transaction-2.1
-bundles=\
  com.ibm.jbatch.spi, \
  com.ibm.ws.security.credentials, \
  com.ibm.websphere.security, \
  com.ibm.jbatch.container.jakarta.ee10, \
  io.openliberty.jakarta.batch.2.1; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.batch:jakarta.batch-api:2.1.1"
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-12.0
WLP-InstantOn-Enabled: true
