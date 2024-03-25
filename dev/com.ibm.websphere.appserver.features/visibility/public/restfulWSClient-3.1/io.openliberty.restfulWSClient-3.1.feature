-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.restfulWSClient-3.1
visibility=public
singleton=true
IBM-API-Package: jakarta.ws.rs; type="spec", \
 jakarta.ws.rs.container; type="spec", \
 jakarta.ws.rs.core; type="spec", \
 jakarta.ws.rs.client; type="spec", \
 jakarta.ws.rs.ext; type="spec", \
 jakarta.ws.rs.sse; type="spec", \
 com.ibm.websphere.jaxrs20.multipart; type="ibm-api", \
 jakarta.xml.bind.annotation; type="internal", \
 jakarta.xml.bind.annotation.adapters; type="internal", \
 org.jboss.resteasy.annotations; type="internal", \
 org.jboss.resteasy.client.jaxrs; type="internal", \
 org.jboss.resteasy.client.jaxrs.internal; type="internal", \
 org.jboss.resteasy.plugins.providers.multipart; type="internal", \
 org.jboss.resteasy.plugins.providers.sse.client; type="internal", \
 org.jboss.resteasy.plugins.providers.sse; type="internal", \
 org.jboss.resteasy.plugins.providers; type="internal", \
 org.jboss.resteasy.spi;type="internal", \
 org.reactivestreams;type="internal", \
 com.ibm.websphere.endpoint; type="ibm-api", \
 jakarta.activation; type="spec", \
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
 com.ibm.wsspi.http, \
 com.ibm.wsspi.http.ee8, \
 com.ibm.wsspi.anno.classsource, \
 com.ibm.wsspi.anno.info, \
 com.ibm.wsspi.anno.service, \
 com.ibm.wsspi.anno.targets, \
 com.ibm.wsspi.anno.util, \
 com.ibm.ws.anno.classsource.specification
IBM-App-ForceRestart: uninstall, \
 install
IBM-ShortName: restfulWSClient-3.1
WLP-AlsoKnownAs: jaxrsClient-3.1
Subsystem-Name: Jakarta RESTful Web Services 3.1 Client
-features=io.openliberty.cdi-4.0, \
  io.openliberty.jakarta.mail-2.1, \
  io.openliberty.jakarta.validation-3.0, \
  com.ibm.websphere.appserver.eeCompatible-10.0, \
  io.openliberty.servlet.internal-6.0, \
  io.openliberty.jakarta.restfulWS-3.1, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0, \
  com.ibm.websphere.appserver.jndi-1.0, \
  io.openliberty.activation.internal-2.1, \
  io.openliberty.jsonp-2.1
-bundles=\
  com.ibm.ws.jaxrs.2.x.config, \
  io.openliberty.org.apache.commons.codec, \
  io.openliberty.org.apache.commons.logging, \
  com.ibm.ws.org.apache.httpcomponents, \
  io.openliberty.org.jboss.logging35, \
  io.openliberty.org.jboss.resteasy.common.ee10
-jars=\
  io.openliberty.jaxrs30; location:="dev/api/ibm/,lib/"
-files=\
  dev/api/ibm/javadoc/io.openliberty.jaxrs30_1.0-javadoc.zip
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
