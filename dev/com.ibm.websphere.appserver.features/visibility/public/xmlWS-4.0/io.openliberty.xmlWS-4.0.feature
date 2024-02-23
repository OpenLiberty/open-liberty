-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWS-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: xmlWS-4.0
Subsystem-Name: Jakarta XML Web Services 4.0
IBM-API-Package: \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal", \
 org.apache.cxf.databinding;type="internal", \
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
 com.ibm.ws.anno.classsource.specification
-features=com.ibm.websphere.appserver.eeCompatible-10.0; ibm.tolerates:="11.0", \
  io.openliberty.xmlws4.0.internal.ee-10.0; ibm.tolerates:="11.0", \
  io.openliberty.xmlws.common-4.0
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.2.3.common.jakarta;start-phase:=CONTAINER_LATE, \
 com.ibm.ws.jaxws.webcontainer.jakarta, \
 com.ibm.ws.jaxws.web.jakarta, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.webservices.javaee.common.jakarta
# These jars are used for the xmlWS scripts below.
-jars=\
 io.openliberty.com.sun.xml.messaging.saaj.2.0, \
 io.openliberty.jakarta.xmlBinding.3.0; location:="dev/api/spec/", \
 io.openliberty.jakarta.activation.2.0; location:="dev/api/spec/", \
 io.openliberty.jakarta.jws.3.0; location:="dev/api/spec/", \
 io.openliberty.jakarta.soap.2.0; location:="dev/api/spec/", \
 io.openliberty.jakarta.xmlWS.3.0; location:="dev/api/spec/", \
 io.openliberty.xmlWS.3.0.internal.tools
-files=\
 bin/xmlWS/wsgen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlWS/wsimport; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 dev/api/ibm/schema/ibm-ws-bnd_1_0.xsd, \
 bin/xmlWS/wsimport.bat, \
 bin/xmlWS/tools/ws-wsimport.jar, \
 bin/xmlWS/wsgen.bat, \
 bin/xmlWS/tools/ws-wsgen.jar
kind=ga
edition=base
WLP-AlsoKnownAs: jaxws-4.0
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true