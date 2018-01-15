-include= ~../cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpRestClient-1.0
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
IBM-ShortName: mpRestClient-1.0
Subsystem-Name: MicroProfile Rest Client 1.0
-features=com.ibm.websphere.appserver.org.eclipse.microprofile.rest.client-1.0
-bundles=com.ibm.ws.require.java8, \
  com.ibm.ws.org.apache.cxf.cxf.rt.rs.mp.client.3.2; apiJar=false; location:="lib/"
kind=noship
edition=core
