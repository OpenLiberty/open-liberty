-include= ~${workspace}/cnf/resources/bnd/feature.props

symbolicName = com.ibm.websphere.appserver.servlet-3.0
visibility = public
singleton = true

IBM-App-ForceRestart: install, uninstall
IBM-ShortName: servlet-3.0
-features: com.ibm.websphere.appserver.javax.servlet-3.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.artifact-1.0, \
 com.ibm.websphere.appserver.javaeedd-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.httptransport-1.0, \
 com.ibm.websphere.appserver.appmanager-1.0, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.anno-1.0, \
 com.ibm.websphere.appserver.javaeePlatform-6.0, \
 com.ibm.websphere.appserver.javaeeCompatible-6.0, \
 com.ibm.websphere.appserver.requestProbes-1.0, \
 com.ibm.websphere.appserver.servlet-servletSpi1.0
-bundles: com.ibm.ws.app.manager.war, \
 com.ibm.ws.app.manager.module, \
 com.ibm.ws.session, \
 com.ibm.ws.org.apache.commons.fileupload, \
 com.ibm.ws.org.apache.commons.io, \
 com.ibm.websphere.security, \
 com.ibm.ws.webcontainer;version="[1.1.0,1.1.100)", \
 com.ibm.ws.webcontainer.servlet.3.0.factories, \
 com.ibm.ws.webcontainer.cors; version="[1,1.0.100)", \
 com.ibm.ws.managedobject, \
 com.ibm.ws.http.plugin.merge, \
 com.ibm.ws.webserver.plugin.runtime, \
 com.ibm.ws.webserver.plugin.runtime.interfaces
-jars: com.ibm.ws.webserver.plugin.utility, \
 com.ibm.websphere.appserver.api.servlet; location:="dev/api/ibm/"

-files: bin/tools/ws-webserverPluginutil.jar, \
 bin/pluginUtility; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/pluginUtility.bat, \
 dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.servlet_1.1-javadoc.zip, \
 dev/api/ibm/schema/ibm-application-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-application-bnd_1_1.xsd, \
 dev/api/ibm/schema/ibm-application-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-application-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-application-ext_1_1.xsd, \
 dev/api/ibm/schema/ibm-common-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-bnd_1_1.xsd, \
 dev/api/ibm/schema/ibm-common-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-common-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-common-ext_1_1.xsd, \
 dev/api/ibm/schema/ibm-web-bnd_1_0.xsd, \
 dev/api/ibm/schema/ibm-web-bnd_1_1.xsd, \
 dev/api/ibm/schema/ibm-web-bnd_1_2.xsd, \
 dev/api/ibm/schema/ibm-web-ext_1_0.xsd, \
 dev/api/ibm/schema/ibm-web-ext_1_1.xsd

IBM-API-Package: javax.servlet.annotation;  type="spec", \
 javax.servlet.descriptor;  type="spec", \
 javax.servlet.resources;  type="spec", \
 javax.servlet.http;  type="spec", \
 javax.servlet;  type="spec", \
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
Subsystem-Name: Java Servlets 3.0
edition=core
kind=ga