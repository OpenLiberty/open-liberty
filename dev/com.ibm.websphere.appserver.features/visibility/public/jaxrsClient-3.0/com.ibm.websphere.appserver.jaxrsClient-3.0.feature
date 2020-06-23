-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.jaxrsClient-3.0
visibility=public
singleton=true
IBM-API-Package: javax.ws.rs; type="spec", \
 javax.ws.rs.container; type="spec", \
 javax.ws.rs.core; type="spec", \
 javax.ws.rs.client; type="spec", \
 javax.ws.rs.ext; type="spec", \
 javax.ws.rs.sse; type="spec", \
 com.ibm.websphere.jaxrs20.multipart; type="ibm-api", \
 com.ibm.websphere.jaxrs.providers.json4j; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: jaxrsClient-3.0
Subsystem-Name: Java RESTful Services Client 3.0
-features=\
 com.ibm.websphere.appserver.javax.jaxrs-2.1
-bundles=\
  io.openliberty.org.jboss.resteasy.common, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.apache.commons.codec.1.3, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
kind=noship
edition=full
WLP-Activation-Type: parallel
