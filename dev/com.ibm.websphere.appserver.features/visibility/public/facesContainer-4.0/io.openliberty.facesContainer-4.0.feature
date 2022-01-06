-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: facesContainer-4.0
WLP-AlsoKnownAs: jsfContainer-4.0
Subsystem-Name: Jakarta Server Faces 4.0 Container
symbolicName=io.openliberty.facesContainer-4.0
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
  io.openliberty.cdi-4.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.validation-3.0, \
  io.openliberty.pages-3.1, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.websocket-2.1
-jars=com.ibm.ws.jsfContainer.jakarta; location:=lib/
kind=noship
edition=full
WLP-Activation-Type: parallel
