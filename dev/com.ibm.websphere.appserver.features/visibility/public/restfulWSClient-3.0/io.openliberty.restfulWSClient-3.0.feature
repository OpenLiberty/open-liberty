-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWSClient-3.0
visibility=public
singleton=true
IBM-API-Package: jakarta.ws.rs; type="spec", \
 jakarta.ws.rs.container; type="spec", \
 jakarta.ws.rs.core; type="spec", \
 jakarta.ws.rs.client; type="spec", \
 jakarta.ws.rs.ext; type="spec", \
 jakarta.ws.rs.sse; type="spec", \
 com.ibm.websphere.jaxrs20.multipart; type="ibm-api", \
 com.ibm.websphere.jaxrs.providers.json4j; type="ibm-api"
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWSClient-3.0
Subsystem-Name: Java RESTful Services Client 3.0
-features=\
 io.openliberty.jakarta.restfulWS-3.0, \
 io.openliberty.jsonp-2.0
-bundles=\
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.apache.commons.codec.1.3, \
  com.ibm.ws.org.apache.commons.logging.1.0.3
#  io.openliberty.org.jboss.resteasy.common, \ # will need to use the transformed version
kind=noship
edition=full
WLP-Activation-Type: parallel
