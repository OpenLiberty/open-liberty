-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messaging.internal-3.1
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: jakarta.jms; version="3.1"; type="spec"
-features=io.openliberty.jakartaeePlatform-10.0, \
  io.openliberty.connectors.internal-2.1, \
  io.openliberty.jakarta.messaging-3.1, \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jakarta.connectors-2.1
-bundles=com.ibm.ws.messaging.jmsspec.common.jakarta
kind=ga
edition=base
WLP-Activation-Type: parallel
