-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.0"; type="spec"
IBM-ShortName: messaging-3.0
IBM-AlsoKnownAs: jms-3.0
Subsystem-Name: Jakarta Message Service 3.0
-features=io.openliberty.messaging-3.0.internal, \
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.connectors-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.jms20.feature
kind=noship
edition=full
