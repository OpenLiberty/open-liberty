-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.scim-2.0
visibility=public
IBM-ShortName: scim-2.0
Subsystem-Name: System for Cross-domain Identity Management 2.0
-features=com.ibm.websphere.appserver.restHandler-1.0, \
 com.ibm.websphere.appserver.servlet-3.0; ibm.tolerates:="3.1,4.0"
-bundles=com.ibm.ws.security.wim.scim.rest.2.0, \
 com.ibm.ws.com.fasterxml.jackson.2.9.1, \
  com.ibm.ws.require.java8
kind=noship
edition=core
