-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging-3.1
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.0"; type="spec"
IBM-ShortName: messaging-3.1
WLP-AlsoKnownAs: jms-3.1
Subsystem-Name: Jakarta Messaging 3.1
-features=io.openliberty.messaging.internal-3.1, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.connectors-2.1, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.jms20.feature
kind=noship
edition=full
WLP-Activation-Type: parallel
