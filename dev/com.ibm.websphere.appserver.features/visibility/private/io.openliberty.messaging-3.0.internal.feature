-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging-3.0.internal
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.0"; type="spec"
-features=io.openliberty.jakartaeePlatform-9.0, \
  io.openliberty.connectors-2.0.internal, \
  io.openliberty.jakarta.messaging-3.0, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jakarta.connectors-2.0
-bundles=com.ibm.ws.messaging.jmsspec.common.jakarta
kind=beta
edition=base
WLP-Activation-Type: parallel
