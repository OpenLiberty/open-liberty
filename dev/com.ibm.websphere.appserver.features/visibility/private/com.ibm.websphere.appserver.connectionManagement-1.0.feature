-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionManagement-1.0
WLP-DisableAllFeatures-OnConflict: false
IBM-API-Package: com.ibm.ws.jca.cm.mbean; type="ibm-api"
visibility=private
-features=com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:="1.2, 2.0", \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.jcaSecurity-1.0, \
 io.openliberty.connectionManager1.0.internal.ee-6.0; ibm.tolerates:="9.0"
-jars=com.ibm.websphere.appserver.api.connectionmanager; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.connectionmanager_1.2-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
