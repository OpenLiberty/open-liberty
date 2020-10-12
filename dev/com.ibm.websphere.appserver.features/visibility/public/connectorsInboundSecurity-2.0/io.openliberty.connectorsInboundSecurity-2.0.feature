-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.connectorsInboundSecurity-2.0
visibility=public
IBM-API-Package: jakarta.security.auth.message.callback; type="spec"
IBM-ShortName: connectorsInboundSecurity-2.0
WLP-AlsoKnownAs: jcaInboundSecurity-2.0
Subsystem-Name: Jakarta EE Connector Architecture Security Inflow 2.0
-features=\
   com.ibm.websphere.appserver.transaction-2.0, \
   com.ibm.websphere.appserver.security-1.0, \
   io.openliberty.connectors-2.0, \
   com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=\
   io.openliberty.connectors.security.internal.inbound, \
   io.openliberty.jakarta.jaspic.2.0; location:=dev/api/spec/; mavenCoordinates="jakarta.security.auth.message:jakarta.security.auth.message-api:2.0.0-RC1"
kind=noship
edition=full
