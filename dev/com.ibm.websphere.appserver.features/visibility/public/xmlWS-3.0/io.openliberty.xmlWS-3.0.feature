-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWS-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: xmlWS-3.0
Subsystem-Name: Jakarta XML Web Services 3.0
IBM-API-Package: \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal", \
 org.apache.cxf.databinding;type="internal", \
 jakarta.activation; type="spec", \
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
 jakarta.xml.ws.wsaddressing; type="spec", \
 jakarta.annotation; type="spec", \
 jakarta.annotation.security; type="spec", \
 jakarta.annotation.sql; type="spec"
IBM-SPI-Package: \
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
 com.ibm.ws.anno.classsource.specification, \
 com.ibm.wsspi.webservices.handler
-features=com.ibm.websphere.appserver.eeCompatible-9.0, \
  com.ibm.websphere.appserver.servlet-5.0, \
  io.openliberty.servlet.internal-5.0, \
  com.ibm.websphere.appserver.globalhandler-2.0, \
  io.openliberty.xmlws.common-3.0
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.2.3.common.jakarta;start-phase:=CONTAINER_LATE, \
 com.ibm.ws.jaxws.webcontainer.jakarta, \
 com.ibm.ws.jaxws.web.jakarta, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.webservices.javaee.common.jakarta, \
 io.openliberty.jaxws.globalhandler.internal.jakarta
-files=\
 bin/xmlWS/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlWS/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/xmlWS/wsimport.bat, \
 bin/xmlWS/tools/ws-wsimport.jar, \
 bin/xmlWS/wsgen.bat, \
 bin/xmlWS/tools/ws-wsgen.jar, \
 dev/spi/ibm/javadoc/io.openliberty.globalhandler.spi_1.0-javadoc.zip
-jars=\
  io.openliberty.globalhandler.spi; location:=dev/spi/ibm/
kind=ga
edition=base
WLP-AlsoKnownAs: jaxws-3.0
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
