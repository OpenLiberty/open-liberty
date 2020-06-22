-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.jca-2.0
visibility=public
singleton=true
IBM-API-Package: com.ibm.ws.jca.service; type="internal"
IBM-ShortName: jca-2.0
Subsystem-Name: Jakarta EE Connector Architecture 2.0
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.jakarta.connector-2.0, \
 io.openliberty.jakartaeePlatform-9.0, \
 io.openliberty.jakarta.internal.jca-1.6, \
 com.ibm.websphere.appserver.transaction-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 io.openliberty.jaxb-3.0
-bundles=com.ibm.ws.app.manager.rar, \
 com.ibm.ws.jca.1.7.jakarta
kind=noship
edition=base
