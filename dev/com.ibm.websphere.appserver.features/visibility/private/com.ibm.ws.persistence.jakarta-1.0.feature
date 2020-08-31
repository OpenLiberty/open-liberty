-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.ws.persistence.jakarta-1.0
visibility=private
IBM-API-Package: com.ibm.websphere.persistence.mbean; type="ibm-api"
-features=com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 io.openliberty.jakarta.annotation-2.0; apiJar=false, \
 com.ibm.websphere.appserver.jdbc-4.1; ibm.tolerates:="4.0, 4.2, 4.3", \
 com.ibm.websphere.appserver.transaction-2.0, \
 com.ibm.websphere.appserver.org.eclipse.persistence-3.0
-bundles=com.ibm.ws.persistence.jakarta, \
 com.ibm.ws.persistence.mbean.jakarta
-jars=com.ibm.websphere.appserver.api.persistence; location:=dev/api/ibm/, \
 com.ibm.ws.persistence.utility.jakarta
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.persistence_1.0-javadoc.zip, \
 bin/tools/ws-generateddlutil.jar, \
 bin/ddlGen.bat, \
 bin/ddlGen; ibm.file.encoding:=ebcdic
kind=beta
edition=base
WLP-Activation-Type: parallel
