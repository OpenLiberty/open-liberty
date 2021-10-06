-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlws.common-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Internal xmlWS 3.2 Common Feature for xmlWS-3.0, xmlWSClient-3.0 server and client features
IBM-API-Package:\
 jakarta.jws; type="spec", \
 jakarta.jws.soap; type="spec", \
 jakarta.xml.soap; type="spec", \
 jakarta.xml.ws; type="spec", \
 jakarta.xml.ws.handler; type="spec", \
 jakarta.xml.ws.handler.soap; type="spec", \
 jakarta.xml.ws.http; type="spec", \
 jakarta.xml.ws.soap; type="spec", \
 jakarta.xml.ws.spi; type="spec", \
 jakarta.xml.ws.spi.http; type="spec", \
 jakarta.xml.ws.wsaddressing; type="spec"
-features=io.openliberty.jakarta.xmlWS-3.0, \
  io.openliberty.xmlBinding-3.0, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  io.openliberty.jakarta.mail-2.0, \
  com.ibm.websphere.appserver.injection-2.0, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=\
 com.ibm.websphere.org.osgi.service.http.jakarta; location:="dev/api/spec/,lib/", \
 com.ibm.ws.cxf.client, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.core.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.features.logging.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.hc.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2.jakarta, \
 com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2.jakarta, \
 com.ibm.ws.org.apache.neethi.3.1.1, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.org.jvnet.mimepull, \
 io.openliberty.xmlWS.3.0.internal.tools, \
 io.openliberty.com.sun.xml.messaging.saaj
kind=beta
edition=base
WLP-Activation-Type: parallel
