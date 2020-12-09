-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWS-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: xmlWS-3.0
IBM-API-Package: \
 jakarta.xml.ws,\
 jakarta.xml.ws.handler,\
 jakarta.xml.ws.handler.soap,\
 jakarta.xml.ws.http,\
 jakarta.xml.ws.soap,\
 jakarta.xml.ws.spi,\
 jakarta.xml.ws.spi.http,\
 jakarta.xml.ws.wsaddressing
Subsystem-Name: Jakarta XML Web Services 3.0
-features=\
 io.openliberty.jakarta.xmlWS-3.0,\
 com.ibm.websphere.appserver.eeCompatible-9.0
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