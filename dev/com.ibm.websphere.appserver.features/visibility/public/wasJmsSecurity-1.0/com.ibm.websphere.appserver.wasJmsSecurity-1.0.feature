-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.wasJmsSecurity-1.0
visibility=public
IBM-ShortName: wasJmsSecurity-1.0
Subsystem-Name: Message Server Security 1.0
-features=\
  com.ibm.websphere.appserver.security-1.0,\
  com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
  com.ibm.websphere.appserver.wasJmsServer-1.0
-bundles=\
  com.ibm.ws.messaging.utils, \
  com.ibm.ws.messaging.security, \
  com.ibm.ws.messaging.security.common
kind=ga
edition=base
WLP-Activation-Type: parallel
