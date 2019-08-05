-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jca-1.7
visibility=public
singleton=true
IBM-API-Package: com.ibm.ws.jca.service; type="internal"
IBM-ShortName: jca-1.7
Subsystem-Name: Java Connector Architecture 1.7
Subsystem-Category: JavaEE7Application
-features=com.ibm.websphere.appserver.javax.connector-1.7, \
 com.ibm.websphere.appserver.javaeePlatform-7.0, \
 com.ibm.websphere.appserver.internal.jca-1.6, \
 com.ibm.websphere.appserver.transaction-1.2, \
 com.ibm.websphere.appserver.javaeeCompatible-7.0; ibm.tolerates:=8.0
-bundles=com.ibm.ws.app.manager.rar, \
 com.ibm.ws.jca.1.7
kind=ga
edition=base
WLP-Activation-Type: parallel
