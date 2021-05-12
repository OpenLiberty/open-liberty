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
 org.jboss.resteasy.annotations; type="internal", \
 org.jboss.resteasy.client.jaxrs; type="internal", \
 org.jboss.resteasy.client.jaxrs.internal; type="internal", \
 org.jboss.resteasy.plugins.providers.sse.client; type="internal", \
 org.jboss.resteasy.plugins.providers.sse; type="internal", \
 org.jboss.resteasy.plugins.providers; type="internal", \
 org.jboss.resteasy.spi;type="internal", \
 org.reactivestreams;type="internal" 
IBM-SPI-Package: com.ibm.wsspi.webservices.handler
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWSClient-3.0
WLP-AlsoKnownAs: jaxrsClient-3.0
Subsystem-Name: Jakarta RESTful Web Services 3.0 Client
-features=\
 com.ibm.websphere.appserver.classloading-1.0, \
 com.ibm.websphere.appserver.containerServices-1.0, \
 com.ibm.websphere.appserver.eeCompatible-9.0, \
 com.ibm.websphere.appserver.injection-2.0, \
 com.ibm.websphere.appserver.jndi-1.0, \
 com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
 com.ibm.websphere.appserver.servlet-5.0, \
 io.openliberty.cdi-3.0, \
 io.openliberty.jakarta.mail-2.0, \
 io.openliberty.jakarta.restfulWS-3.0, \
 io.openliberty.jakarta.validation-3.0, \
 io.openliberty.jsonp-2.0, \
 com.ibm.websphere.appserver.globalhandler-1.0
# com.ibm.websphere.appserver.internal.optional.jaxb-2.2; ibm.tolerates:=2.3, \ # not sure about these...
# com.ibm.websphere.appserver.internal.optional.jaxws-2.2; ibm.tolerates:=2.3, \
-bundles=\
  com.ibm.ws.jaxrs.2.x.config, \
  io.openliberty.org.apache.commons.codec, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.ws.org.apache.httpcomponents, \
  com.ibm.ws.org.jboss.logging, \
  io.openliberty.org.jboss.resteasy.common.jakarta, \
  io.openliberty.restfulWS.internal.globalhandler.jakarta, \
  com.ibm.websphere.org.eclipse.microprofile.config.1.4; location:="dev/api/stable/,lib/"; mavenCoordinates="org.eclipse.microprofile.config:microprofile-config-api:1.4"
kind=beta
edition=core
WLP-Activation-Type: parallel
