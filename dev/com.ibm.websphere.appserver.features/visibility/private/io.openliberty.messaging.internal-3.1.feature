-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging.internal-3.1
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.0"; type="spec"
-features=io.openliberty.jakartaeePlatform-10.0, \
  io.openliberty.connectors-2.1.internal, \
  io.openliberty.jakarta.messaging-3.1, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jakarta.connectors-2.1
-bundles=com.ibm.ws.messaging.jmsspec.common.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel
