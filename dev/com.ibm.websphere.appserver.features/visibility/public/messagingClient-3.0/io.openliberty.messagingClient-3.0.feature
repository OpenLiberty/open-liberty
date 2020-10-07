-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messagingClient-3.0
visibility=public
singleton=true
IBM-API-Package: jakarta.jms; version="3.0"; type="spec", \
 com.ibm.websphere.sib.api.jms; type="internal"
IBM-ShortName: messagingClient-3.0
WLP-AlsoKnownAs: wasJmsClient-3.0
Subsystem-Name: Jakarta Messaging 3.0 Client for Message Server
-features=io.openliberty.messaging-3.0.internal, \
 com.ibm.websphere.appserver.channelfw-1.0, \
 com.ibm.websphere.appserver.transaction-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.messaging.common, \
 com.ibm.ws.resource, \
 com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.jms.common.jakarta, \
 com.ibm.ws.messaging.jms.2.0.jakarta, \
 com.ibm.ws.messaging.comms.client
kind=noship
edition=full
