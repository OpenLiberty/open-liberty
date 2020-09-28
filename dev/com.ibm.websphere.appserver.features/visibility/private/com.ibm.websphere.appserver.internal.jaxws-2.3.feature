-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.internal.jaxws-2.3
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
install
Subsystem-Name: Internal Java Web Services 2.3
-features=\
 com.ibm.websphere.appserver.injection-1.0, \
 com.ibm.websphere.appserver.servlet-4.0, \
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.javax.mail-1.6, \
 com.ibm.websphere.appserver.globalhandler-1.0, \
 com.ibm.websphere.appserver.httpcommons-1.0, \
 com.ibm.websphere.appserver.internal.cxf.common-3.2, \
 com.ibm.websphere.appserver.internal.slf4j-1.7.7
-bundles=\
 com.ibm.ws.com.sun.xml.messaging.saaj; require-java:="9", \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.cxf.client, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.transports.http.hc.3.2, \
 com.ibm.ws.org.jvnet.mimepull, \
 com.ibm.websphere.javaee.jws.1.0; require-java:="9"; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.jws:jsr181-api:1.0-MR1",\
 com.ibm.websphere.javaee.jaxws.2.3; location:="dev/api/spec/"; apiJar=false,\
 com.ibm.ws.jaxws.tools.2.2.10, \
 com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
 com.ibm.ws.prereq.wsdl4j.1.6.2
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
