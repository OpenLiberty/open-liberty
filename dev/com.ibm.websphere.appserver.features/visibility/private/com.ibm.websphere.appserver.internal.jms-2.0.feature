-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jms-2.0
singleton=true
IBM-App-ForceRestart: uninstall
IBM-API-Package: javax.jms; version="2.0"; type="spec"
-features=com.ibm.websphere.appserver.javax.connector.internal-1.7, \
 com.ibm.websphere.appserver.javaeePlatform-7.0, \
 com.ibm.websphere.appserver.internal.jca-1.6, \
 com.ibm.websphere.appserver.transaction-1.2
-bundles=com.ibm.websphere.javaee.jms.2.0; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.jms:javax.jms-api:2.0", \
 com.ibm.ws.messaging.jmsspec.common
kind=ga
edition=base
WLP-Activation-Type: parallel
