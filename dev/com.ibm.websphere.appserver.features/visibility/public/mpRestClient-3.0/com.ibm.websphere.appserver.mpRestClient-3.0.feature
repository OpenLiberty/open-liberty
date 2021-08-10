-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient-3.0
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
  org.reactivestreams; type="stable";
IBM-ShortName: mpRestClient-3.0
Subsystem-Name: MicroProfile Rest Client 3.0

#  com.ibm.websphere.appserver.mpConfig-3.0, \
-features=io.openliberty.jsonp-2.0, \
  io.openliberty.restfulWSClient-3.0, \
  io.openliberty.mpCompatible-5.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-3.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0
  
-bundles=io.openliberty.org.jboss.resteasy.mprestclient.jakarta; apiJar=false; location:="lib/"
kind=noship
edition=core
WLP-Activation-Type: parallel

#TODO: update MP Config API feature dependency to 3.0 when available
