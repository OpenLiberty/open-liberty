-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors-2.1
visibility=public
singleton=true
IBM-ShortName: connectors-2.1
WLP-AlsoKnownAs: jca-2.1
Subsystem-Name: Jakarta Connectors 2.1
Subsystem-Category: JakartaEE10Application
-features=io.openliberty.appserver.connectors-2.1, \
  io.openliberty.jakartaeePlatform-10.0, \
  io.openliberty.connectors-2.1.internal, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.app.manager.rar, \
 com.ibm.ws.jca.1.7.jakarta
kind=noship
edition=full
WLP-Activation-Type: parallel
