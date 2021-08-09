-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.sipServlet-1.1
WLP-DisableAllFeatures-OnConflict: false
visibility=public
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: javax.servlet.sip.annotation;  type="spec", \
 javax.servlet.sip;  type="spec", \
 javax.servlet.sip.ar;  type="spec", \
 javax.servlet.sip.ar.spi;  type="spec", \
 com.ibm.websphere.sip.resolver.events;  type="ibm-api", \
 com.ibm.websphere.sip.resolver.exception;  type="ibm-api", \
 com.ibm.websphere.sip.resolver;  type="ibm-api", \
 com.ibm.websphere.sip.unmatchedMessages.events;  type="ibm-api", \
 com.ibm.websphere.sip.unmatchedMessages;  type="ibm-api", \
 com.ibm.websphere.sip;  type="ibm-api"
IBM-ShortName: sipServlet-1.1
Subsystem-Name: SIP Servlet 1.1
-features=com.ibm.websphere.appserver.channelfw-1.0, \
  com.ibm.websphere.appserver.servlet-3.1; ibm.tolerates:="3.0,4.0"
-bundles=\
  com.ibm.websphere.javaee.jaxb.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.websphere.javaee.jaxws.2.2; apiJar=false; require-java:="9"; location:="dev/api/spec/,lib/",\
  com.ibm.ws.sipcontainer, \
  com.ibm.ws.app.manager.sar
-jars=com.ibm.websphere.appserver.api.sipServlet.1.1; location:="dev/api/ibm/,lib/", \
 com.ibm.websphere.javaee.servlet.sip.1.1; location:="dev/api/spec/,lib/"; mavenCoordinates="com.ibm.ws.java:com.ibm.ws.java.sipServlet.1.1:1.0.14"
-files=dev/api/ibm/javadoc/com.ibm.websphere.appserver.api.sipServlet.1.1_1.0-javadoc.zip
kind=ga
edition=base
