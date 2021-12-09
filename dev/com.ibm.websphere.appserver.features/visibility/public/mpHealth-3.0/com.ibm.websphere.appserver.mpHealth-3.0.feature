-include= ~${workspace}/cnf/resources/bnd/feature.props
symbolicName=com.ibm.websphere.appserver.mpHealth-3.0
visibility=public
singleton=true
IBM-App-ForceRestart: install, \
 uninstall
IBM-API-Package: \
  org.eclipse.microprofile.health;  type="stable", \
  org.eclipse.microprofile.health.spi;  type="stable"
IBM-ShortName: mpHealth-3.0
Subsystem-Name: MicroProfile Health 3.0
-features=com.ibm.websphere.appserver.json-1.0, \
  com.ibm.websphere.appserver.mpConfig-2.0, \
  com.ibm.wsspi.appserver.webBundle-1.0, \
  com.ibm.websphere.appserver.servlet-4.0, \
  io.openliberty.mpCompatible-4.0, \
  com.ibm.websphere.appserver.org.eclipse.microprofile.health-3.0, \
  com.ibm.websphere.appserver.cdi-2.0, \
  com.ibm.websphere.appserver.jndi-1.0
-bundles=com.ibm.websphere.jsonsupport, \
 io.openliberty.microprofile.health.3.0.internal; apiJar=false; location:="lib/", \
 com.ibm.ws.org.joda.time.1.6.2
kind=ga
edition=core
WLP-Activation-Type: parallel
