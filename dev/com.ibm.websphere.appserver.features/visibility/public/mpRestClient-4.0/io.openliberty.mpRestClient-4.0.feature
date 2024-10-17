-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpRestClient-4.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.rest.client; type="stable", \
  org.eclipse.microprofile.rest.client.annotation; type="stable", \
  org.eclipse.microprofile.rest.client.ext; type="stable", \
  org.eclipse.microprofile.rest.client.inject; type="stable", \
  org.eclipse.microprofile.rest.client.spi; type="stable", \
  org.reactivestreams; type="stable", \
  org.jboss.resteasy.client.jaxrs.internal; type="internal", \
  org.jboss.resteasy.client.jaxrs.internal.proxy; type="internal", \
  org.jboss.resteasy.microprofile.client; type="internal"
IBM-ShortName: mpRestClient-4.0
Subsystem-Name: MicroProfile Rest Client 4.0

-features=\
  io.openliberty.mpCompatible-7.0, \
  io.openliberty.jsonp-2.1, \
  io.openliberty.mpConfig-3.1, \
  io.openliberty.mpRestClient.internal.cdi-4.0; ibm.tolerates:="4.1", \
  io.openliberty.org.eclipse.microprofile.rest.client-4.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0

-bundles=\
  io.openliberty.org.jboss.resteasy.mprestclient.4.0; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
WLP-InstantOn-Enabled: true
WLP-Platform: microProfile-7.0
