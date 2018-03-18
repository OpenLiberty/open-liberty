-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsClient-2.2
visibility=private
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
-features=com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:="1.6", \
 com.ibm.websphere.appserver.jaxb-2.2
-bundles=com.ibm.ws.org.apache.cxf-rt-transports-http.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-core.2.6.2, \
 com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-soap.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-xml.2.6.2, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.org.apache.cxf-rt-databinding-jaxb.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-management.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-addr.2.6.2, \
 com.ibm.websphere.javaee.jaxws.2.2; location:="dev/api/spec/,lib/", \
 com.ibm.ws.org.apache.cxf-rt-frontend-simple.2.6.2, \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.ws.jaxws.clientcontainer, \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/", \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.org.apache.cxf-rt-frontend-jaxws.2.6.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-policy.2.6.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
 com.ibm.ws.org.apache.cxf-api.2.6.2, \
 com.ibm.ws.jaxws.tools.2.2.10
-files=bin/jaxws/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxws/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/jaxws/wsimport.bat, \
 bin/jaxws/tools/ws-wsimport.jar, \
 bin/jaxws/wsgen.bat, \
 bin/jaxws/tools/ws-wsgen.jar
kind=ga
edition=base