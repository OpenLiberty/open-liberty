-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlws.common-3.0
visibility=private
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-Process-Types: client, \
 server
Subsystem-Name: Internal xmlWS 3.2 Common Feature for xmlWS-3.0, xmlWSClient-3.0 server and client features
IBM-API-Package:\
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
 jakarta.xml.ws.wsaddressing; type="spec"
-features=io.openliberty.xmlws.impl-3.0, \
  io.openliberty.xmlBinding-3.0
kind=ga
edition=base
WLP-Activation-Type: parallel
