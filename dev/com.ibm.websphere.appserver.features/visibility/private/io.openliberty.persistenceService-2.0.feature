-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.persistenceService-2.0
visibility=private
IBM-API-Package: com.ibm.websphere.persistence.mbean; type="ibm-api"
-features=com.ibm.websphere.appserver.jdbc-4.2; ibm.tolerates:="4.3", \
  com.ibm.websphere.appserver.containerServices-1.0, \
  io.openliberty.jakarta.annotation-2.0; apiJar=false, \
  com.ibm.websphere.appserver.org.eclipse.persistence-3.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.persistence.jakarta, \
 com.ibm.ws.persistence.mbean.jakarta, \
 com.ibm.websphere.appserver.api.persistence.jakarta; location:="dev/api/ibm/", \
 com.ibm.ws.persistence.utility.jakarta
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.persistence_1.0-javadoc.zip, \
 bin/tools/ws-generateddlutil.jar, \
 bin/ddlGen.bat, \
 bin/ddlGen; ibm.file.encoding:=ebcdic
kind=beta
edition=base
WLP-Activation-Type: parallel
