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
  io.openliberty.jaxws.common-2.2
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.jaxws.2.3.common; start-phase:=CONTAINER_LATE, \
 com.ibm.ws.webservices.javaee.common, \
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
WLP-Platform: javaee-6.0,javaee-7.0,javaee-8.0,jakartaee-8.0
