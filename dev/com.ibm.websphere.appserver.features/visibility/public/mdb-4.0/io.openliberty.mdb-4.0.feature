-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mdb-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-ShortName: mdb-4.0
IBM-API-Package: com.ibm.ws.ejbcontainer.mdb; type="internal"
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.jakartaeePlatform-9.0, \
 io.openliberty.ejbCore-2.0, \
 io.openliberty.jakarta.ejb-4.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.connectors-2.0, \
 io.openliberty.jakarta.interceptor-2.0
-bundles=com.ibm.ws.ejbcontainer.mdb.jakarta, \
 io.openliberty.ejbcontainer.v40.internal
Subsystem-Name: Message-Driven Beans 4.0
kind=beta
edition=base
WLP-Activation-Type: parallel
