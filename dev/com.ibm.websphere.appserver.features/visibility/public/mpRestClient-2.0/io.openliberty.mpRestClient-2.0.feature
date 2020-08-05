-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=io.openliberty.mpRestClient-2.0
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
IBM-ShortName: mpRestClient-2.0
Subsystem-Name: MicroProfile Rest Client 2.0
-features=io.openliberty.org.eclipse.microprofile.rest.client-2.0, \
 com.ibm.websphere.appserver.javax.cdi-2.0, \
 com.ibm.websphere.appserver.javax.annotation-1.3, \
 com.ibm.websphere.appserver.jaxrsClient-2.1, \
 com.ibm.websphere.appserver.jsonp-1.1, \
 io.openliberty.mpConfig-2.0
-bundles=com.ibm.ws.org.apache.cxf.cxf.rt.rs.mp.client.3.3; apiJar=false; location:="lib/"
kind=noship
edition=full
WLP-Activation-Type: parallel
