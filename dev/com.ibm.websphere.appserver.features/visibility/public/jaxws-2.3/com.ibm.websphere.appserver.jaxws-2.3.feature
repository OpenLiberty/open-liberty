-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxws-2.3
visibility=public
singleton=true
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxws-2.3
IBM-API-Package: \
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
 javax.jws; type="spec"; require-java:="9"
Subsystem-Name: Java Web Services 2.3
-features=\
 com.ibm.websphere.appserver.jaxb-2.3, \
 com.ibm.websphere.appserver.internal.jaxws-2.3
-bundles=\
 com.ibm.websphere.javaee.jaxws.2.3; location:="dev/api/spec/"; mavenCoordinates="javax.xml.ws:jaxws-api:2.3.0"
kind=noship
edition=full
