-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient-1.4
WLP-DisableAllFeatures-OnConflict: false
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
 org.eclipse.microprofile.rest.client; type="stable", \
 org.eclipse.microprofile.rest.client.annotation; type="stable", \
 org.eclipse.microprofile.rest.client.ext; type="stable", \
 org.eclipse.microprofile.rest.client.inject; type="stable", \
 org.eclipse.microprofile.rest.client.spi; type="stable"
IBM-ShortName: mpRestClient-1.4
Subsystem-Name: MicroProfile Rest Client 1.4
-features=com.ibm.websphere.appserver.mpConfig-1.4; ibm.tolerates:="1.3", \
  io.openliberty.mpCompatible-0.0, \
  com.ibm.websphere.appserver.jsonp-1.1, \
  com.ibm.websphere.appserver.javax.annotation-1.3, \
  com.ibm.websphere.appserver.jaxrsClient-2.1, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-1.4, \
  com.ibm.websphere.appserver.javax.cdi-2.0, \
  com.ibm.websphere.appserver.org.reactivestreams.reactive-streams-1.0
-bundles=\
 com.ibm.ws.org.apache.cxf.cxf.rt.rs.mp.client.3.3; apiJar=false; location:="lib/"
kind=ga
edition=core
WLP-Activation-Type: parallel
