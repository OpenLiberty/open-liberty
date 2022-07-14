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
 org.apache.cxf.databinding;type="internal"
-features=com.ibm.websphere.appserver.eeCompatible-10.0, \
  com.ibm.websphere.appserver.servlet-6.0, \
  com.ibm.websphere.appserver.globalhandler-1.0, \
  io.openliberty.xmlws.common-4.0
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.common.jakarta;start-phase:=CONTAINER_LATE, \
 com.ibm.ws.jaxws.webcontainer.jakarta, \
 com.ibm.ws.jaxws.web.jakarta, \
 com.ibm.ws.jaxws.wsat, \
 com.ibm.ws.webservices.javaee.common.jakarta
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
kind=noship
edition=full
WLP-AlsoKnownAs: jaxws-4.0
WLP-Activation-Type: parallel
