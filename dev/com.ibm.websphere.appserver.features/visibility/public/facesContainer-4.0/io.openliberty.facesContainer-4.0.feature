-include= ~${workspace}/cnf/resources/bnd/feature.props
IBM-ShortName: facesContainer-4.0
WLP-AlsoKnownAs: jsfContainer-4.0
Subsystem-Name: Jakarta Faces 4.0 Container
symbolicName=io.openliberty.facesContainer-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-API-Package: org.jboss.weld;type="internal",\
  org.jboss.weld.manager;type="internal",\
  org.jboss.weld.context.http;type="internal",\
  org.jboss.weld.context;type="internal"
-bundles=\
  io.openliberty.facesContainer.4.0.internal.classloading
-features=io.openliberty.facesProvider-4.0.0.Container, \
  io.openliberty.cdi-4.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.jakarta.validation-3.0, \
  io.openliberty.expressionLanguage-5.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.websocket-2.1
-jars=com.ibm.ws.jsfContainer.jakarta; location:=lib/
kind=beta
edition=core
WLP-Activation-Type: parallel
