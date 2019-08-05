-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wasJmsClient-2.0
visibility=public
singleton=true
IBM-API-Package: javax.jms; version="2.0"; type="spec", \
 com.ibm.websphere.sib.api.jms; type="internal"
IBM-ShortName: wasJmsClient-2.0
Subsystem-Name: JMS 2.0 Client for Message Server
-features=com.ibm.websphere.appserver.internal.jms-2.0, \
 com.ibm.websphere.appserver.channelfw-1.0, \
 com.ibm.websphere.appserver.transaction-1.2
-bundles=com.ibm.ws.messaging.common, \
 com.ibm.ws.resource, \
 com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.jms.common, \
 com.ibm.ws.messaging.jms.2.0, \
 com.ibm.ws.messaging.comms.client
kind=ga
edition=base
WLP-Activation-Type: parallel
