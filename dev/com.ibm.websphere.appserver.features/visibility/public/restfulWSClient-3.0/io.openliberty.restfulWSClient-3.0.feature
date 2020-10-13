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
WLP-AlsoKnownAs: jaxrsClient-3.0
Subsystem-Name: Java RESTful Services Client 3.0
-features=\
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 com.ibm.websphere.appserver.injection-2.0, \
 com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.org.eclipse.microprofile.config-1.4, \
 com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 io.openliberty.cdi-3.0, \
 io.openliberty.jakarta.restfulWS-3.0, \
 io.openliberty.jakarta.validation-3.0, \
 io.openliberty.jsonp-2.0, \
 io.openliberty.mail-2.0
# com.ibm.websphere.appserver.globalhandler-1.0, \ # hard dependency on javax.servlet, etc.
# com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \ # not sure about these...
# com.ibm.websphere.appserver.internal.optional.jaxws-2.2; ibm.tolerates:=2.3, \
-bundles=\
  com.ibm.ws.org.apache.commons.codec.1.3, \
  com.ibm.ws.org.apache.commons.logging.1.0.3, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.jboss.logging, \
  io.openliberty.org.jboss.resteasy.common.jakarta
kind=beta
edition=core
WLP-Activation-Type: parallel
