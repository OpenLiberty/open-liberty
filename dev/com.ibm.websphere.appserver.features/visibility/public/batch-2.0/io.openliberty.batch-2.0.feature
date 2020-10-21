-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.batch-2.0
WLP-DisableAllFeatures-OnConflict: false
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
  jakarta.inject;  type="spec"
IBM-ShortName: batch-2.0
Subsystem-Name: Jakarta Batch API 2.0
-features=\
  io.openliberty.jakarta.jaxb-3.0, \
  io.openliberty.jakarta.cdi-3.0,\
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.jakarta.annotation-2.0, \
  io.openliberty.persistence-3.0, \
  io.openliberty.persistenceService-2.0, \
  com.ibm.websphere.appserver.contextService-1.0, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3, 4.1", \
  com.ibm.websphere.appserver.transaction-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
  com.ibm.jbatch.spi, \
  com.ibm.ws.security.credentials, \
  com.ibm.websphere.security, \
  com.ibm.jbatch.container.jakarta, \
  io.openliberty.jakarta.batch-2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="jakarta.batch:jakarta.batch-api:2.0.0", \
  com.ibm.websphere.appserver.thirdparty.eclipselink.3.0; location:=dev/api/third-party/; mavenCoordinates="org.eclipse.persistence:eclipselink:3.0.0"
  
kind=noship
edition=base
WLP-Activation-Type: parallel
