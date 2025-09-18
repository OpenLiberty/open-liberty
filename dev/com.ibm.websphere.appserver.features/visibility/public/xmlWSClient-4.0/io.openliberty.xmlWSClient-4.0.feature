-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.xmlWSClient-4.0
visibility=public
Subsystem-Name: Jakarta XML Web Services 4.0 Client
WLP-Activation-Type: parallel
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
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
IBM-ShortName: xmlWSClient-4.0
IBM-Process-Types: client
-features=\
  io.openliberty.xmlWSClient.internal-4.0
kind=beta
edition=base
