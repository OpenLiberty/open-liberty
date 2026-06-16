-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: facesContainer-5.0
WLP-AlsoKnownAs: jsfContainer-5.0
Subsystem-Name: Jakarta Faces 5.0 Container
symbolicName=io.openliberty.facesContainer-5.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: org.jboss.weld;type="internal",\
  org.jboss.weld.manager;type="internal",\
  org.jboss.weld.context.http;type="internal",\
  org.jboss.weld.context;type="internal"
-bundles=\
  io.openliberty.facesContainer.4.1.internal.classloading
-features=io.openliberty.facesProvider-5.0.0.Container, \
  io.openliberty.cdi-5.0, \
  com.ibm.websphere.appserver.servlet-6.2, \
  com.ibm.websphere.appserver.eeCompatible-12.0, \
  io.openliberty.jakarta.validation-4.0, \
  io.openliberty.expressionLanguage-6.1, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.websocket-2.3
-jars=com.ibm.ws.jsfContainer.jakarta; location:=lib/
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-Platform: jakartaee-12.0
