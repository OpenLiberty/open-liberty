-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jca-2.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.ws.jca.service; type="internal"
IBM-ShortName: jca-2.0
Subsystem-Name: Jakarta EE Connector Architecture 2.0
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.connector-2.0, \
 com.ibm.websphere.appserver.transaction-2.0
kind=noship
edition=full
