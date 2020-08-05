-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.messagingSecurity-2.0
visibility=public
IBM-ShortName: messagingSecurity-2.0
Subsystem-Name: Message Server Security 2.0
-features=com.ibm.websphere.appserver.security-1.0,\
 com.ibm.websphere.appserver.transaction-2.0, \
 io.openliberty.messagingServer-2.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0
-bundles=com.ibm.ws.messaging.utils, \
 com.ibm.ws.messaging.security, \
 com.ibm.ws.messaging.security.common
kind=noship
edition=full
