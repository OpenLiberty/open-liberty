-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.spnego-1.0
visibility=public
IBM-ShortName: spnego-1.0
Subsystem-Name: Simple and Protected GSSAPI Negotiation Mechanism 1.0
-features=com.ibm.websphere.appserver.appSecurity-2.0; ibm.tolerates:=3.0, \
  com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.authFilter-1.0
-bundles=com.ibm.ws.security.spnego, \
  com.ibm.ws.security.token.s4u2, \
  com.ibm.ws.webcontainer.security.provider
kind=ga
edition=core
