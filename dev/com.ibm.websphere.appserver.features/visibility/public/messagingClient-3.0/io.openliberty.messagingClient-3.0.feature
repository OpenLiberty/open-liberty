-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messagingClient-3.0
visibility=public
singleton=true
IBM-API-Package: jakarta.jms; type="spec", \
 com.ibm.websphere.sib.api.jms; type="internal", \
 com.ibm.websphere.endpoint; type="ibm-api", \
 com.ibm.ws.jca.cm.mbean; type="ibm-api", \
 jakarta.resource.spi; type="spec", \
 jakarta.resource.cci; type="spec", \
 jakarta.resource.spi.endpoint; type="spec", \
 jakarta.resource; type="spec", \
 jakarta.resource.spi.work; type="spec", \
 jakarta.resource.spi.security; type="spec"
IBM-ShortName: messagingClient-3.0
WLP-AlsoKnownAs: wasJmsClient-3.0
Subsystem-Name: Messaging Server 3.0 Client
-features=com.ibm.websphere.appserver.channelfw-1.0, \
  io.openliberty.messaging.internal-3.0; ibm.tolerates:="3.1", \
  com.ibm.websphere.appserver.eeCompatible-9.0; ibm.tolerates:="10.0, 11.0", \
  com.ibm.websphere.appserver.transaction-2.0, \
  io.openliberty.jakarta.messaging-3.0; ibm.tolerates:="3.1"
-bundles=com.ibm.ws.messaging.common, \
 com.ibm.ws.resource, \
 com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.security.common, \
 com.ibm.ws.messaging.jms.common.jakarta, \
 com.ibm.ws.messaging.jms.2.0.jakarta, \
 com.ibm.ws.messaging.comms.client, \
 io.openliberty.io.netty, \
 io.openliberty.io.netty.ssl, \
 io.openliberty.netty.internal, \
 io.openliberty.netty.internal.impl
kind=ga
edition=base
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true; type:=beta
