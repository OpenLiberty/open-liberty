-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.2
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: javax.xml.ws.handler; type="spec", \
 javax.xml.ws.http; type="spec", \
 javax.xml.ws.spi; type="spec", \
 javax.xml.ws.handler.soap; type="spec", \
 javax.xml.ws.wsaddressing; type="spec", \
 javax.xml.ws.spi.http; type="spec", \
 javax.xml.ws; type="spec", \
 javax.xml.ws.soap; type="spec", \
 javax.wsdl.extensions.http; type="spec", \
 javax.wsdl.extensions.mime; type="spec", \
 javax.wsdl.extensions.schema; type="spec", \
 javax.wsdl.extensions.soap; type="spec", \
 javax.wsdl.extensions.soap12; type="spec", \
 javax.wsdl.extensions; type="spec", \
 javax.wsdl.factory; type="spec", \
 javax.wsdl.xml; type="spec", \
 javax.wsdl; type="spec", \
 org.apache.cxf.databinding;type="internal", \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal"
IBM-ShortName: jaxws-2.2
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
Subsystem-Name: Java Web Services 2.2
-features=com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1, 4.0"; apiJar=false, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:=1.6, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.jaxb-2.2
-bundles=com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.org.apache.cxf-rt-transports-http.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-core.2.6.2, \
 com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-soap.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-xml.2.6.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.org.apache.cxf-rt-databinding-jaxb.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-management.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-addr.2.6.2, \
 com.ibm.ws.jaxws.common, \
 com.ibm.websphere.javaee.jaxws.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.2.12", \
 com.ibm.ws.jaxws.tools.2.2.10, \
 com.ibm.ws.org.apache.cxf-rt-frontend-simple.2.6.2, \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.org.apache.cxf-rt-frontend-jaxws.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-policy.2.6.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.org.apache.cxf-api.2.6.2
-files=bin/jaxws/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxws/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/jaxws/wsimport.bat, \
 bin/jaxws/tools/ws-wsimport.jar, \
 bin/jaxws/wsgen.bat, \
 bin/jaxws/tools/ws-wsgen.jar
kind=ga
edition=base
