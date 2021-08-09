-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectorsInboundSecurity-2.0
visibility=public
IBM-API-Package: jakarta.security.auth.message.callback; type="spec"
IBM-ShortName: connectorsInboundSecurity-2.0
WLP-AlsoKnownAs: jcaInboundSecurity-2.0
Subsystem-Name: Jakarta Connectors 2.0 Inbound Security
-features=io.openliberty.jakarta.authentication-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.connectors-2.0, \
  com.ibm.websphere.appserver.security-1.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=\
   io.openliberty.connectors.security.internal.inbound
kind=beta
edition=base
