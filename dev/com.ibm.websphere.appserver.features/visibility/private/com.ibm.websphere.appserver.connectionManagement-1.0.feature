-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.connectionManagement-1.0
IBM-API-Package: com.ibm.ws.jca.cm.mbean; type="ibm-api"
visibility=private
-features=com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.jcaSecurity-1.0
-bundles=com.ibm.ws.jca.cm
-jars=com.ibm.websphere.appserver.api.connectionmanager; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.connectionmanager_1.1-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
