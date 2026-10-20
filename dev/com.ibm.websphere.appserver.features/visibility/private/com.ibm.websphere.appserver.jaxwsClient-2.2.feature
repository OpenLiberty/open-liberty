-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxwsClient-2.2
WLP-DisableAllFeatures-OnConflict: false
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client
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
  javax.wsdl; type="spec"
-features=\
  io.openliberty.jaxws.common-2.2
-bundles=\
  com.ibm.ws.jaxws.2.3.clientcontainer
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
