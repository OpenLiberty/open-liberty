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
 org.apache.cxf.databinding;type="internal"
-features=\
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 io.openliberty.xmlws.common-3.0
-bundles=\
 com.ibm.ws.javaee.ddmodel.ws, \
 com.ibm.ws.jaxws.webcontainer.jakarta, \
 com.ibm.ws.jaxws.2.3.common.jakarta;start-phase:=CONTAINER_LATE, \
 com.ibm.ws.jaxws.2.3.web.jakarta, \
 com.ibm.ws.jaxws.2.3.wsat, \
 com.ibm.ws.webservices.javaee.common.jakarta, \
 com.ibm.ws.webservices.handler.jakarta
-jars=com.ibm.websphere.appserver.spi.globalhandler.jakarta; location:=dev/spi/ibm/
-files=\
 bin/xmlBinding/xjc.bat, \
 bin/xmlBinding/tools/ws-schemagen.jar, \
 bin/xmlBinding/schemagen; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlBinding/xjc; ibm.executable:=true; ibm.file.encoding:=ebcdic, \
 bin/xmlBinding/tools/ws-xjc.jar, \
 bin/xmlBinding/schemagen.bat
kind=noship
edition=full
WLP-AlsoKnownAs: jaxws-3.0
WLP-Activation-Type: parallel