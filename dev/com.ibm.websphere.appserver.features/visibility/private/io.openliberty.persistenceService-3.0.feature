-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistenceService-3.0
singleton=true
visibility=private
IBM-API-Package: com.ibm.websphere.persistence.mbean; type="ibm-api"
-features=com.ibm.websphere.appserver.eeCompatible-12.0, \
  com.ibm.websphere.appserver.transaction-2.1, \
  com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  io.openliberty.jakarta.annotation-3.0; apiJar=false, \
  io.openliberty.org.hibernate-4.0, \
  io.openliberty.jakarta.activation-2.2, \
  io.openliberty.xmlBinding.internal-4.0, \
  com.ibm.websphere.appserver.jndi-1.0
-bundles=io.openliberty.persistence.service.hibernate, \
 io.openliberty.org.jboss.logging36, \
 io.openliberty.io.smallrye.jandex3, \
 io.openliberty.net.bytebuddy, \
 io.openliberty.org.antlr4.runtime, \
 io.openliberty.org.hibernate.models, \
 io.openliberty.org.hibernate.models.bytebuddy, \
 com.ibm.ws.persistence.mbean, \
 com.ibm.websphere.appserver.api.persistence; location:="dev/api/ibm/", \
 com.ibm.ws.persistence.utility
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.persistence_1.0-javadoc.zip, \
 bin/tools/ws-generateddlutil.jar, \
 bin/ddlGen.bat, \
 bin/ddlGen; ibm.file.encoding:=ebcdic
kind=noship
edition=full
WLP-Activation-Type: parallel
