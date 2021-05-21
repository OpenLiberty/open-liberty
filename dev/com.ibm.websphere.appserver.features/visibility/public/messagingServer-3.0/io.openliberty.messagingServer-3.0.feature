-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messagingServer-3.0
visibility=public
IBM-API-Package: com.ibm.websphere.messaging.mbean; type="ibm-api"
IBM-ShortName: messagingServer-3.0
WLP-AlsoKnownAs: wasJmsServer-3.0
Subsystem-Name: Messaging Server 3.0
-features=com.ibm.websphere.appserver.appLifecycle-1.0, \
  com.ibm.websphere.appserver.channelfw-1.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.transaction-2.0
-bundles=com.ibm.ws.messaging.comms.server, \
 com.ibm.ws.messaging.msgstore.jakarta, \
 com.ibm.ws.messaging.common, \
 com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.security.common, \
 com.ibm.ws.messaging.runtime, \
 com.ibm.ws.messaging.comms.client, \
 com.ibm.websphere.security
-jars=com.ibm.websphere.appserver.api.messaging; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.messaging_1.0-javadoc.zip
kind=beta
edition=base
WLP-Activation-Type: parallel
