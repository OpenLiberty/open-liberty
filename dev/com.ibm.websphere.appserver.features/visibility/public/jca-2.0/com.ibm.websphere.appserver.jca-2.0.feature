-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jca-2.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.ws.jca.service; type="internal"
IBM-ShortName: jca-2.0
Subsystem-Name: Java Connector Architecture 2.0
Subsystem-Category: JakartaEE9Application
-features=com.ibm.websphere.appserver.jakarta.connector-2.0, \
 com.ibm.websphere.appserver.transaction-2.0
kind=noship
edition=full
