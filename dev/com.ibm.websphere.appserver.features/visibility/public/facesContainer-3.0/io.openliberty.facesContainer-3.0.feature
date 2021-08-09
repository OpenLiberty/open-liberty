-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: facesContainer-3.0
WLP-AlsoKnownAs: jsfContainer-3.0
Subsystem-Name: Jakarta Server Faces 3.0 Container
symbolicName=io.openliberty.facesContainer-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: org.jboss.weld;type="internal",\
  org.jboss.weld.manager;type="internal",\
  org.jboss.weld.context.http;type="internal",\
  org.jboss.weld.context;type="internal"
-bundles=\
  io.openliberty.facesContainer.classloading.3.0
-features=io.openliberty.facesProvider-3.0.0.Container, \
  io.openliberty.cdi-3.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  io.openliberty.jakarta.validation-3.0, \
  io.openliberty.pages-3.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.websocket-2.0
-jars=com.ibm.ws.jsfContainer.jakarta; location:=lib/
kind=beta
edition=core
WLP-Activation-Type: parallel
