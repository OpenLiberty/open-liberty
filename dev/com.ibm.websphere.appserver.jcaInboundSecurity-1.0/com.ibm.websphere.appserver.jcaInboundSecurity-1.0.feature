-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jcaInboundSecurity-1.0
visibility=public
IBM-API-Package: javax.security.auth.message.callback; type="spec"
IBM-ShortName: jcaInboundSecurity-1.0
Subsystem-Name: Java Connector Architecture Security Inflow
-features=com.ibm.websphere.appserver.transaction-1.1; ibm.tolerates:=1.2, \
 com.ibm.websphere.appserver.security-1.0, \
 com.ibm.websphere.appserver.jca-1.6; ibm.tolerates:=1.7
-bundles=com.ibm.ws.jca.inbound.security, \
 com.ibm.websphere.javaee.jaspic.1.1; location:=dev/api/spec/
kind=ga
edition=base
