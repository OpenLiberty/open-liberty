-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.3
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
 javax.xml.soap:javax.xml.soap-api:1.4.0, \
 com.sun.xml.messaging.saaj:saaj-impl:1.3.28, \
 org.jvnet.mimepull:mimepull:1.9.7, \
 org.jvnet.staxex:stax-ex:1.7.7, \
IBM-ShortName: jaxws-2.3
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
Subsystem-Name: Java Web Services 2.3
-features=\
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.javax.servlet-3.0; ibm.tolerates:="3.1, 4.0"; apiJar=false, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:=1.6, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.jaxb-2.3
-bundles=\
 com.ibm.ws.org.apache.cxf.cxf.core.3.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-soap.3.2, \
 com.ibm.ws.org.apache.cxf-rt-bindings-xml.3.2, \
 com.ibm.ws.org.apache.cxf-rt-databinding-jaxb.3.2, \
 com.ibm.ws.org.apache.cxf.cxf-rt-frontend-jaxws.3.2, \
 com.ibm.ws.org.apache.cxf-rt-frontend-simple.3.2, \
 com.ibm.ws.org.apache.cxf-rt-management.3.2, \
 com.ibm.ws.org.apache.cxf-rt-transports-http.3.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-addr.3.2, \
 com.ibm.ws.org.apache.cxf-rt-ws-policy.3.2, \
 com.ibm.ws.org.apache.xml.resolver.1.2, \
 com.ibm.ws.jaxws.common, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.org.apache.neethi.3.0.2, \
 com.ibm.websphere.javaee.jaxws.2.3; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.2.12", \
 com.ibm.websphere.javaee.jws.1.0; required-osgi-ee:="(&(osgi.ee=JavaSE)(version>=9))"; location:="dev/api/spec/,lib/",\
 com.ibm.ws.jaxws.tools.2.2.10, \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 com.ibm.ws.org.apache.ws.xmlschema.core.2.0.3, \
-files=\
 bin/jaxws/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxws/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/jaxws/wsimport.bat, \
 bin/jaxws/tools/ws-wsimport.jar, \
 bin/jaxws/wsgen.bat, \
 bin/jaxws/tools/ws-wsgen.jar
kind=noship
edition=base
