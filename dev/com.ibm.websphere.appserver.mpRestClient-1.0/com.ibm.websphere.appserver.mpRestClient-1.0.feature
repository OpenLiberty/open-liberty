-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient-1.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.rest.client;  type="stable", \
  org.eclipse.microprofile.rest.client.annotation;  type="stable", \
  org.eclipse.microprofile.rest.client.inject;  type="stable", \
IBM-ShortName: mpRestClient-1.0
Subsystem-Name: MicroProfile Rest Client 1.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-1.0, \
 com.ibm.websphere.appserver.javax.jaxrs-2.0; ibm.tolerates:=2.1, \
 com.ibm.websphere.appserver.javax.cdi-1.2; ibm.tolerates:=2.0, \
 com.ibm.websphere.appserver.javax.annotation-1.2
-bundles=com.ibm.ws.require.java8
kind=beta
edition=core
