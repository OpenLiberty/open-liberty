-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: facesContainer-4.1
WLP-AlsoKnownAs: jsfContainer-4.1
Subsystem-Name: Jakarta Faces 4.1 Container
symbolicName=io.openliberty.facesContainer-4.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: org.jboss.weld;type="internal",\
  org.jboss.weld.manager;type="internal",\
  org.jboss.weld.context.http;type="internal",\
  org.jboss.weld.context;type="internal"
-bundles=\
  io.openliberty.facesContainer.4.1.internal.classloading
-features=io.openliberty.facesProvider-4.1.0.Container, \
  io.openliberty.cdi-4.1, \
  com.ibm.websphere.appserver.servlet-6.1, \
  com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.jakarta.validation-3.1, \
  io.openliberty.expressionLanguage-6.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.websocket-2.2
-jars=com.ibm.ws.jsfContainer.jakarta; location:=lib/
kind=noship
edition=full
WLP-Activation-Type: parallel
