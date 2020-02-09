-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsClient-2.3
visibility=private
IBM-Process-Types: client
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
  javax.jws; type="spec"; require-java:="9",\
  javax.jws.soap; type="spec"; require-java:="9",\
  javax.xml.soap; type="spec"; require-java:="9",\
  javax.xml.ws.handler; type="spec",\
  javax.xml.ws.http; type="spec",\
  javax.xml.ws.spi; type="spec",\
  javax.xml.ws.handler.soap; type="spec",\
  javax.xml.ws.wsaddressing; type="spec",\
  javax.xml.ws.spi.http; type="spec",\
  javax.xml.ws; type="spec",\
  javax.xml.ws.soap; type="spec",\
  javax.wsdl.extensions.http; type="spec",\
  javax.wsdl.extensions.mime; type="spec",\
  javax.wsdl.extensions.schema; type="spec",\
  javax.wsdl.extensions.soap; type="spec",\
  javax.wsdl.extensions.soap12; type="spec",\
  javax.wsdl.extensions; type="spec",\
  javax.wsdl.factory; type="spec",\
  javax.wsdl.xml; type="spec",\
  javax.wsdl; type="spec", \
  org.apache.cxf.binding.soap.wsdl.extensions;type="internal", \
  org.apache.cxf.databinding;type="internal"
-features=\
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.internal.cxf.common-3.2, \
  com.ibm.websphere.appserver.containerServices-1.0, \
  com.ibm.websphere.appserver.classloading-1.0, \
  com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:="1.6", \
  com.ibm.websphere.appserver.jaxb-2.3, \
  com.ibm.websphere.appserver.internal.slf4j-1.7.7, \
  com.ibm.websphere.appserver.servlet-4.0, \
  com.ibm.websphere.appserver.optional.corba-1.5, \
  com.ibm.websphere.appserver.iiopcommon-1.0
-bundles=\
  com.ibm.ws.jaxws.2.3.clientcontainer, \
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2, \
  com.ibm.ws.javaee.ddmodel.wsbnd, \
  com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2, \
  com.ibm.websphere.javaee.jaxws.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.3.0", \
  com.ibm.websphere.javaee.jws.1.0; location:="dev/api/spec/,lib/"; require-java:="9",\
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2, \
  com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
  com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
  com.ibm.ws.prereq.wsdl4j.1.6.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2, \
  com.ibm.ws.jaxws.tools.2.2.10, \
  com.ibm.ws.com.sun.xml.messaging.saaj; require-java:="9",\
  com.ibm.ws.org.jvnet.mimepull; require-java:="9", \
  com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2, \
  com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2, \
  com.ibm.ws.cxf.client
-files=\
  bin/jaxws/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  bin/jaxws/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
  dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
  bin/jaxws/wsimport.bat, \
  bin/jaxws/tools/ws-wsimport.jar, \
  bin/jaxws/wsgen.bat, \
  bin/jaxws/tools/ws-wsgen.jar
kind=noship
edition=full