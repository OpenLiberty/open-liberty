-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: jsfContainer-2.3
Subsystem-Name: JavaServer Faces Container 2.3
symbolicName=com.ibm.websphere.appserver.jsfContainer-2.3
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: org.jboss.weld;type="internal",\
  org.jboss.weld.manager;type="internal",\
  org.jboss.weld.context.http;type="internal",\
  org.jboss.weld.context;type="internal"
-bundles=\
  com.ibm.ws.jsfContainer.classloading.2.3
-features=com.ibm.websphere.appserver.websocket-1.1, \
  com.ibm.websphere.appserver.jsfProvider-2.3.0.Container, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.jsp-2.3, \
  com.ibm.websphere.appserver.eeCompatible-8.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  com.ibm.websphere.appserver.javax.validation-2.0
-jars=com.ibm.ws.jsfContainer; location:=lib/
kind=ga
edition=core
