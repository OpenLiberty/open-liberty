-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.servlet-5.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: servlet-5.0
IBM-API-Package: jakarta.servlet.annotation;  type="spec", \
 jakarta.servlet.descriptor;  type="spec", \
 jakarta.servlet.resources;  type="spec", \
 jakarta.servlet.http;  type="spec", \
 jakarta.servlet;  type="spec", \
 com.ibm.websphere.servlet.session;  type="ibm-api", \
 com.ibm.wsspi.servlet.session;  type="ibm-api", \
 com.ibm.websphere.servlet.error;  type="ibm-api", \
 com.ibm.websphere.servlet.container;  type="ibm-api", \
 com.ibm.websphere.servlet.context;  type="ibm-api", \
 com.ibm.websphere.servlet.event;  type="ibm-api", \
 com.ibm.websphere.webcontainer;  type="ibm-api", \
 com.ibm.wsspi.webcontainer;  type="internal", \
 com.ibm.wsspi.webcontainer.annotation;  type="internal"
IBM-SPI-Package: com.ibm.wsspi.webcontainer, \
 com.ibm.wsspi.webcontainer.collaborator, \
 com.ibm.wsspi.webcontainer.extension, \
 com.ibm.wsspi.webcontainer.filter, \
 com.ibm.wsspi.webcontainer.metadata, \
 com.ibm.wsspi.webcontainer.osgi.extension, \
 com.ibm.wsspi.webcontainer.servlet, \
 com.ibm.wsspi.webcontainer.webapp, \
 com.ibm.websphere.servlet.filter, \
 com.ibm.websphere.servlet.response, \
 com.ibm.ws.webcontainer.extension, \
 com.ibm.websphere.servlet.request, \
 com.ibm.ws.webcontainer.spiadapter.collaborator, \
 com.ibm.websphere.webcontainer.async
Subsystem-Category: JakartaEE9Application
-features=io.openliberty.jakartaeePlatform-9.0, \
  io.openliberty.servlet.api-5.0, \
  com.ibm.websphere.appserver.javaeeddSchema-1.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.injection-2.0, \
  com.ibm.websphere.appserver.servlet-servletSpi1.0, \
  com.ibm.websphere.appserver.httptransport-1.0, \
  com.ibm.websphere.appserver.javaeedd-1.0, \
  com.ibm.websphere.appserver.appmanager-1.0, \
  com.ibm.websphere.appserver.requestProbes-1.0, \
  io.openliberty.jakarta.annotation-2.0, \
  com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.anno-2.0, \
  com.ibm.websphere.appserver.artifact-1.0
-bundles=com.ibm.ws.app.manager.war.jakarta, \
 com.ibm.ws.managedobject, \
 com.ibm.ws.org.apache.commons.io, \
 com.ibm.websphere.security, \
 com.ibm.ws.org.apache.commons.fileupload.jakarta, \
 com.ibm.ws.webcontainer.servlet.4.0.jakarta, \
 com.ibm.ws.webcontainer.servlet.4.0.factories.jakarta, \
 com.ibm.ws.webcontainer.servlet.3.1.jakarta, \
 com.ibm.ws.session.jakarta, \
 com.ibm.ws.webcontainer.jakarta, \
 com.ibm.ws.webcontainer.cors.jakarta, \
 com.ibm.ws.http.plugin.merge, \
 com.ibm.ws.webserver.plugin.runtime.jakarta, \
 com.ibm.ws.webserver.plugin.runtime.interfaces
-jars=com.ibm.ws.webserver.plugin.utility, \
 com.ibm.websphere.appserver.api.servlet; location:=dev/api/ibm/
-files=bin/tools/ws-webserverPluginutil.jar, \
 bin/pluginUtility; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/pluginUtility.bat, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.servlet_1.1-javadoc.zip
Subsystem-Name: Jakarta Servlet 5.0
kind=beta
edition=core
WLP-Activation-Type: parallel
