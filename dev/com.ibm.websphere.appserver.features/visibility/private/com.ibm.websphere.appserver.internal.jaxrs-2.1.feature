-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jaxrs-2.1
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
Subsystem-Name: Internal Java RESTful Services 2.1
-features=com.ibm.websphere.appserver.javax.jaxrs-2.1, \
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.6, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.javaeeCompatible-8.0
-bundles=com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.jaxrs.2.1.common, \
 com.ibm.ws.jaxrs.2.x.config, \
 com.ibm.ws.org.apache.cxf.cxf.core.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxrs.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.rs.client.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.rs.service.description.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.rs.sse.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.wadlto.jaxrs.3.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.jaxrs.2.0.tools, \
 com.ibm.ws.jaxrs.2.0.web, \
 com.ibm.ws.jaxrs.2.0.server, \
 com.ibm.ws.jaxrs.2.0.client
-files=bin/jaxrs/wadl2java, \
 bin/jaxrs/wadl2java.bat, \
 bin/jaxrs/tools/wadl2java.jar
kind=ga
edition=core
