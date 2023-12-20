-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
WLP-Activation-Type: parallel
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxws-2.2
IBM-SPI-Package: com.ibm.wsspi.webservices.handler, \
 com.ibm.wsspi.adaptable.module, \
 com.ibm.ws.adaptable.module.structure, \
 com.ibm.wsspi.adaptable.module.adapters, \
 com.ibm.wsspi.artifact, \
 com.ibm.wsspi.artifact.factory, \
 com.ibm.wsspi.artifact.factory.contributor, \
 com.ibm.wsspi.artifact.overlay, \
 com.ibm.wsspi.artifact.equinox.module, \
 com.ibm.wsspi.anno.classsource, \
 com.ibm.wsspi.anno.info, \
 com.ibm.wsspi.anno.service, \
 com.ibm.wsspi.anno.targets, \
 com.ibm.wsspi.anno.util, \
 com.ibm.ws.anno.classsource.specification
IBM-API-Package:  \
 javax.jws.soap; type="spec"; require-java:="9", \
 javax.wsdl; type="spec", \
 javax.wsdl.extensions; type="spec", \
 javax.wsdl.extensions.http; type="spec", \
 javax.wsdl.extensions.mime; type="spec", \
 javax.wsdl.extensions.schema; type="spec", \
 javax.wsdl.extensions.soap; type="spec", \
 javax.wsdl.extensions.soap12; type="spec", \
 javax.wsdl.factory; type="spec", \
 javax.wsdl.xml; type="spec", \
 javax.xml.soap; type="spec"; require-java:="9", \
 javax.xml.ws; type="spec", \
 javax.xml.ws.handler; type="spec", \
 javax.xml.ws.handler.soap; type="spec", \
 javax.xml.ws.http; type="spec", \
 javax.xml.ws.soap; type="spec", \
 javax.xml.ws.spi; type="spec", \
 javax.xml.ws.spi.http; type="spec", \
 javax.xml.ws.wsaddressing; type="spec", \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal", \
 org.apache.cxf.databinding;type="internal", \
 javax.jws; type="spec"; require-java:="9", \
 javax.annotation; type="spec", \
 javax.annotation.security; type="spec", \
 javax.annotation.sql; type="spec"
Subsystem-Name: Java Web Services 2.2
-features=\
  io.openliberty.servlet.api-3.0; apiJar=false; ibm.tolerates:="3.1,4.0", \
  com.ibm.websphere.appserver.globalhandler-1.0, \
  com.ibm.websphere.appserver.httpcommons-1.0, \
  com.ibm.websphere.appserver.injection-1.0, \
  com.ibm.websphere.appserver.jaxb-2.2, \
  com.ibm.websphere.appserver.internal.slf4j-1.7, \
  com.ibm.websphere.appserver.internal.cxf.common-3.2, \
  com.ibm.websphere.appserver.internal.optional.jaxws-2.2, \
  com.ibm.websphere.appserver.javax.mail-1.5; ibm.tolerates:="1.6"
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
 com.ibm.ws.webservices.javaee.common, \
 com.ibm.websphere.javaee.jws.1.0; require-java:="9"; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.jws:jsr181-api:1.0-MR1", \
 com.ibm.websphere.javaee.jaxws.2.2; location:="dev/api/spec/,lib/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.2.12", \
 com.ibm.websphere.javaee.wsdl4j.1.2; location:="dev/api/spec/,lib/"; mavenCoordinates="wsdl4j:wsdl4j:1.6.3", \
 com.ibm.websphere.prereq.wsdl4j.api; location:="dev/api/spec/,lib/", \
 com.ibm.ws.com.sun.xml.messaging.saaj; require-java:="9", \
 com.ibm.ws.javaee.ddmodel.wsbnd, \
 com.ibm.ws.jaxws.tools.2.2.10, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.soap.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.bindings.xml.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.databinding.jaxb.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.features.logging.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.jaxws.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.frontend.simple.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.management.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.common.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.tools.validator.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.wsdl.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.addr.3.2, \
 com.ibm.ws.org.apache.cxf.cxf.rt.ws.policy.3.2, \
 com.ibm.ws.org.jvnet.mimepull;require-java:="9", \
 com.ibm.ws.prereq.wsdl4j.1.6.2, \
 io.openliberty.jaxws.globalhandler.internal
-files=\
 bin/jaxws/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/jaxws/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/jaxws/wsimport.bat, \
 bin/jaxws/tools/ws-wsimport.jar, \
 bin/jaxws/wsgen.bat, \
 bin/jaxws/tools/ws-wsgen.jar
kind=ga
edition=base
WLP-InstantOn-Enabled: true
