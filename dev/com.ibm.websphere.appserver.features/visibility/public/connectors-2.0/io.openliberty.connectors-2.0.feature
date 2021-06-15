-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectors-2.0
visibility=public
singleton=true
IBM-ShortName: connectors-2.0
WLP-AlsoKnownAs: jca-2.0
Subsystem-Name: Jakarta Connectors 2.0
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.appserver.connectors-2.0, \
  io.openliberty.jakartaeePlatform-9.0, \
  io.openliberty.connectors-2.0.internal, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.app.manager.rar.jakarta, \
 com.ibm.ws.jca.1.7.jakarta
kind=beta
edition=base
WLP-Activation-Type: parallel
