-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.servlet-6.1
visibility=public
singleton=true
IBM-App-ForceRestart: install, uninstall
IBM-ShortName: servlet-6.1
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
Subsystem-Category: JakartaEE11Application
-features=com.ibm.websphere.appserver.eeCompatible-11.0, \
  io.openliberty.servlet.internal-6.1, \
  io.openliberty.servlet-servletSpi2.0
-jars=io.openliberty.servlet; location:=dev/api/ibm/
-files=dev/api/ibm/javadoc/io.openliberty.servlet_1.1-javadoc.zip
Subsystem-Name: Jakarta Servlet 6.1
kind=noship
edition=full
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
