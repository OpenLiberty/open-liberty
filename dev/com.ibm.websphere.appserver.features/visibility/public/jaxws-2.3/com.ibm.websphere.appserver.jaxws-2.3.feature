-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.3
visibility=public
IBM-App-ForceRestart: uninstall, \
 install
IBM-API-Package: \
 javax.jws; type="spec"; require-java:="9", \
 javax.jws.soap; type="spec"; require-java:="9", \
 javax.xml.soap; type="spec"; require-java:="9", \
 javax.xml.ws.handler; type="spec", \
 javax.xml.ws.http; type="spec", \
 javax.xml.ws.spi; type="spec", \
 javax.xml.ws.handler.soap; type="spec", \
 javax.xml.ws.wsaddressing; type="spec", \
 javax.xml.ws.spi.http; type="spec", \
 javax.xml.ws; type="spec", \
 javax.xml.ws.soap; type="spec", \
 javax.wsdl.extensions.http; type="spec", \
 javax.wsdl.extensions.mime; type="spec", \
 javax.wsdl.extensions.schema; type="spec", \
 javax.wsdl.extensions.soap; type="spec", \
 javax.wsdl.extensions.soap12; type="spec", \
 javax.wsdl.extensions; type="spec", \
 javax.wsdl.factory; type="spec", \
 javax.wsdl.xml; type="spec", \
 javax.wsdl; type="spec", \
 org.apache.cxf.databinding;type="internal", \
 org.apache.cxf.binding.soap.wsdl.extensions;type="internal"
IBM-ShortName: jaxws-2.3
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
Subsystem-Name: Java Web Services 2.3
-features=\
 com.ibm.websphere.appserver.internal.jaxws-2.3, \
 com.ibm.websphere.appserver.jaxb-2.3
kind=noship
edition=full
